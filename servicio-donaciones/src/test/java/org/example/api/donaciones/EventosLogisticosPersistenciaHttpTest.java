package org.example.api.donaciones;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.persistence.*;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.*;
import org.example.dominio.notificacion.*;
import org.junit.jupiter.api.Test;
import org.example.scheduler.ProcesadorNotificacionesPendientes;

import static org.junit.jupiter.api.Assertions.*;

class EventosLogisticosPersistenciaHttpTest {

  @Test
  void reconoceEventoConNuevoControllerYEntityManager() throws Exception {
    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory(
            "simple-persistence-unit",
            Map.of(
                "javax.persistence.jdbc.driver",
                "org.hsqldb.jdbc.JDBCDriver",
                "javax.persistence.jdbc.url",
                "jdbc:hsqldb:mem:eventos_" + UUID.randomUUID(),
                "javax.persistence.jdbc.user", "sa",
                "javax.persistence.jdbc.password", "",
                "hibernate.dialect",
                "org.hibernate.dialect.HSQLDialect",
                "hibernate.hbm2ddl.auto", "create-drop"
            )
        );

    AtomicInteger notificaciones = new AtomicInteger();

    Notificador notificador =
        new Notificador(new Email(), new SMS(), new WhatsApp()) {
          @Override
          public Notificacion enviarPendiente(
              NotificacionPendiente pendiente
          ) {
            int intento = notificaciones.incrementAndGet();

            Notificacion resultado = new Notificacion(
                pendiente.getDestinatario(),
                pendiente.getMensaje()
            );

            if (intento == 1) {
              resultado.marcarComoFallida();
            } else {
              resultado.marcarComoCompletada();
            }

            return resultado;
          }
        };

    ObjectMapper json =
        new ObjectMapper().findAndRegisterModules();

    try {
      String idAsignacion;
      EventoLogistico evento;

      EntityManager em = factory.createEntityManager();

      try {
        var repo = new RepositorioAsignacionesDonacion(em);

        var donacion =
            new Donacion("Arroz", 1, "kg", null, null, null, null);

        var asignacion = donacion.asignarA(
            new EntidadBeneficiaria("Comedor", "Calle", "123")
        );

        donacion.cambiarEstado(
            EstadoDonacion.LISTA_PARA_ENTREGAR, "Lista"
        );

        repo.agregar(asignacion);
        idAsignacion = asignacion.getIdAsString();

        evento = new EventoLogistico(
            UUID.randomUUID().toString(),
            EventoLogistico.Tipo.INICIO_TRASLADO,
            List.of(new EventoLogistico.Referencia(
                donacion.getIdAsString(),
                asignacion.getEntidad().getIdAsString()
            )),
            "ABC",
            "Inicio",
            LocalDateTime.now()
        );
      } finally {
        em.close();
      }

      // Cada envío crea una aplicación y un controller nuevos.
      // Cada petición abre su propio EntityManager.
      for (int intento = 0; intento < 2; intento++) {
        Javalin app = crearApp(factory, notificador, json);

        try {
          app.start(0);

          HttpRequest request = HttpRequest.newBuilder(
                  URI.create(
                      "http://localhost:" + app.port()
                          + "/interno/logistica/eventos"
                  )
              )
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(
                  json.writeValueAsString(evento)
              ))
              .build();

          HttpResponse<String> respuesta = HttpClient.newHttpClient()
              .send(request, HttpResponse.BodyHandlers.ofString());

          assertEquals(
              204,
              respuesta.statusCode(),
              "Intento " + (intento + 1) + ": " + respuesta.body()
          );
        } finally {
          app.stop();
        }
      }

      // Registrar el evento solamente encola: todavía no hubo envíos.
      assertEquals(0, notificaciones.get());

      var procesador =
          new ProcesadorNotificacionesPendientes(factory, notificador);

// Primer intento: el proveedor simulado falla.
      procesador.ejecutar(100);

      EntityManager controlFallo = factory.createEntityManager();

      try {
        var pendientes = new RepositorioAsignacionesDonacion(controlFallo)
            .buscarNotificacionesDeEvento(evento.idOperacion());

        // Esta donación del test no tiene donante:
        // solamente se genera el mensaje para la entidad.
        assertEquals(1, pendientes.size());
        assertEquals(
            EstadoNotificacion.FALLIDA,
            pendientes.get(0).getEstado()
        );
        assertEquals(1, pendientes.get(0).getIntentos());
      } finally {
        controlFallo.close();
      }

// Segundo intento: se completa.
      procesador.ejecutar(100);

// Tercera ejecución: no debe volver a enviar lo completado.
      procesador.ejecutar(100);

      assertEquals(2, notificaciones.get());

      // Lectura final desde otro EntityManager.
      EntityManager verificacion = factory.createEntityManager();

      try {
        var repo =
            new RepositorioAsignacionesDonacion(verificacion);

        Donacion recuperada = repo.buscarPorId(idAsignacion)
            .orElseThrow()
            .getDonacion();

        assertEquals(
            EstadoDonacion.EN_TRASLADO,
            recuperada.getEstadoActual()
        );

        // Asignación, lista para entregar e inicio del traslado.
        assertEquals(3, recuperada.getHistorialEstados().size());

        assertTrue(
            repo.buscarEventoProcesado(evento.idOperacion()).isPresent()
        );

        var pendientes =
            repo.buscarNotificacionesDeEvento(evento.idOperacion());

        assertEquals(1, pendientes.size());
        assertEquals(
            EstadoNotificacion.COMPLETADA,
            pendientes.get(0).getEstado()
        );
        assertEquals(2, pendientes.get(0).getIntentos());
        assertEquals(2, notificaciones.get());
      } finally {
        verificacion.close();
      }
    } finally {
      factory.close();
    }
  }

  private Javalin crearApp(
      EntityManagerFactory factory,
      Notificador notificador,
      ObjectMapper json
  ) {
    ThreadLocal<EntityManager> actual = new ThreadLocal<>();

    var asignaciones =
        new RepositorioAsignacionesDonacion(actual::get);

    var donaciones =
        new RepositorioDonaciones(actual::get);

    var controller = new IntegracionLogisticaController(
        asignaciones, donaciones, notificador
    );

    Javalin app = Javalin.create(
        config -> config.jsonMapper(new JavalinJackson(json, false))
    );

    app.exception(Exception.class, (e, ctx) -> {
      e.printStackTrace();

      Throwable causa = e;
      while (causa.getCause() != null && causa.getCause() != causa) {
        causa = causa.getCause();
      }

      ctx.status(500).result(
          causa.getClass().getName() + ": " + causa.getMessage()
      );
    });

    app.before(ctx -> actual.set(factory.createEntityManager()));

    app.after(ctx -> {
      EntityManager em = actual.get();

      try {
        if (em != null && em.isOpen()) {
          try {
            if (em.getTransaction().isActive()) {
              em.getTransaction().rollback();
            }
          } finally {
            em.close();
          }
        }
      } finally {
        actual.remove();
      }
    });

    app.post(
        "/interno/logistica/eventos",
        controller::registrarEvento
    );

    return app;
  }

  @Test
  void rechazaTodoElLoteSiUnaDonacionNoEstaLista() throws Exception {
    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory(
            "simple-persistence-unit",
            Map.of(
                "javax.persistence.jdbc.driver",
                "org.hsqldb.jdbc.JDBCDriver",
                "javax.persistence.jdbc.url",
                "jdbc:hsqldb:mem:lote_" + UUID.randomUUID(),
                "javax.persistence.jdbc.user", "sa",
                "javax.persistence.jdbc.password", "",
                "hibernate.dialect",
                "org.hibernate.dialect.HSQLDialect",
                "hibernate.hbm2ddl.auto", "create-drop"
            )
        );

    AtomicInteger notificaciones = new AtomicInteger();

    Notificador notificador =
        new Notificador(new Email(), new SMS(), new WhatsApp()) {
          @Override
          public void notificarEventoLogistico(
              AsignacionDonacion asignacion,
              EventoLogistico evento
          ) {
            notificaciones.incrementAndGet();
          }
        };

    ObjectMapper json =
        new ObjectMapper().findAndRegisterModules();

    try {
      String idPrimera;
      String idSegunda;
      EventoLogistico evento;

      EntityManager preparacion = factory.createEntityManager();

      try {
        var repo = new RepositorioAsignacionesDonacion(preparacion);

        var primera =
            new Donacion("Arroz", 1, "kg", null, null, null, null);

        var asignacionPrimera = primera.asignarA(
            new EntidadBeneficiaria("Comedor A", "Calle 1", "123")
        );

        primera.cambiarEstado(
            EstadoDonacion.LISTA_PARA_ENTREGAR, "Lista"
        );

        repo.agregar(asignacionPrimera);

        var segunda =
            new Donacion("Fideos", 2, "kg", null, null, null, null);

        var asignacionSegunda = segunda.asignarA(
            new EntidadBeneficiaria("Comedor B", "Calle 2", "456")
        );

        // Esta queda en ASIGNACION_REALIZADA.
        repo.agregar(asignacionSegunda);

        idPrimera = asignacionPrimera.getIdAsString();
        idSegunda = asignacionSegunda.getIdAsString();

        evento = new EventoLogistico(
            UUID.randomUUID().toString(),
            EventoLogistico.Tipo.INICIO_TRASLADO,
            List.of(
                new EventoLogistico.Referencia(
                    primera.getIdAsString(),
                    asignacionPrimera.getEntidad().getIdAsString()
                ),
                new EventoLogistico.Referencia(
                    segunda.getIdAsString(),
                    asignacionSegunda.getEntidad().getIdAsString()
                )
            ),
            "ABC",
            "Inicio del recorrido",
            LocalDateTime.now()
        );
      } finally {
        preparacion.close();
      }

      Javalin app = crearApp(factory, notificador, json);

      try {
        app.start(0);

        HttpRequest request = HttpRequest.newBuilder(
                URI.create(
                    "http://localhost:" + app.port()
                        + "/interno/logistica/eventos"
                )
            )
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                json.writeValueAsString(evento)
            ))
            .build();

        HttpResponse<String> respuesta = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(409, respuesta.statusCode(), respuesta.body());

        EntityManager verificacion = factory.createEntityManager();

        try {
          var repo =
              new RepositorioAsignacionesDonacion(verificacion);

          Donacion primera = repo.buscarPorId(idPrimera)
              .orElseThrow()
              .getDonacion();

          Donacion segunda = repo.buscarPorId(idSegunda)
              .orElseThrow()
              .getDonacion();

          assertEquals(
              EstadoDonacion.LISTA_PARA_ENTREGAR,
              primera.getEstadoActual()
          );
          assertEquals(2, primera.getHistorialEstados().size());

          assertEquals(
              EstadoDonacion.ASIGNACION_REALIZADA,
              segunda.getEstadoActual()
          );
          assertEquals(1, segunda.getHistorialEstados().size());

          assertTrue(
              repo.buscarEventoProcesado(evento.idOperacion()).isEmpty()
          );

          assertEquals(0, notificaciones.get());

          assertTrue(
              repo.buscarNotificacionesDeEvento(evento.idOperacion()).isEmpty()
          );
        } finally {
          verificacion.close();
        }
      } finally {
        app.stop();
      }
    } finally {
      factory.close();
    }
  }
}