package org.example.dominio.donacion;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.*;
import javax.persistence.*;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donante.*;
import org.example.dominio.notificacion.*;
import org.example.scheduler.ProcesadorNotificacionesPendientes;
import org.junit.jupiter.api.*;

class SeguimientoLogisticoTest {

  private EntityManagerFactory factory;
  private Notificador notificador;
  private final List<String> mensajes = new ArrayList<>();

  @BeforeEach
  void preparar() {
    factory = Persistence.createEntityManagerFactory(
        "simple-persistence-unit",
        Map.of(
            "javax.persistence.jdbc.driver",
            "org.hsqldb.jdbc.JDBCDriver",
            "javax.persistence.jdbc.url",
            "jdbc:hsqldb:mem:seguimiento_" + UUID.randomUUID(),
            "javax.persistence.jdbc.user", "sa",
            "javax.persistence.jdbc.password", "",
            "hibernate.dialect",
            "org.hibernate.dialect.HSQLDialect",
            "hibernate.hbm2ddl.auto", "create-drop"
        )
    );

    mensajes.clear();

    notificador = new Notificador(
        new Email(), new SMS(), new WhatsApp()
    ) {
      @Override
      public Notificacion enviarPendiente(
          NotificacionPendiente pendiente
      ) {
        mensajes.add(pendiente.getMensaje());

        Notificacion resultado = new Notificacion(
            pendiente.getDestinatario(),
            pendiente.getMensaje()
        );

        resultado.marcarComoCompletada();
        return resultado;
      }
    };
  }

  @AfterEach
  void cerrar() {
    if (factory != null && factory.isOpen()) {
      factory.close();
    }
  }

  private record Datos(
      String idAsignacion,
      String idDonacion,
      String idEntidad
  ) {
  }

  private Datos asignar(boolean lista) {
    EntityManager em = factory.createEntityManager();

    try {
      em.getTransaction().begin();

      PersonaHumana donante = new PersonaHumana(
          UUID.randomUUID() + "@example.org",
          "123",
          TipoDocumento.DNI,
          "Juan",
          "Perez",
          30,
          Genero.MASCULINO,
          "Calle 1"
      );

      EntidadBeneficiaria entidad =
          new EntidadBeneficiaria("Comedor", "Calle 1", "123");

      em.persist(donante);
      em.persist(entidad);

      Donacion donacion =
          new Donacion("Arroz", 10, "kg", null, null, null, donante);

      em.persist(donacion);

      AsignacionDonacion asignacion = donacion.asignarA(entidad);

      if (lista) {
        donacion.cambiarEstado(
            EstadoDonacion.LISTA_PARA_ENTREGAR, "Lista"
        );
      }

      new RepositorioAsignacionesDonacion(em).agregar(asignacion);

      // Incluye el cambio a LISTA_PARA_ENTREGAR.
      em.getTransaction().commit();

      return new Datos(
          asignacion.getIdAsString(),
          donacion.getIdAsString(),
          entidad.getIdAsString()
      );
    } finally {
      try {
        if (em.getTransaction().isActive()) {
          em.getTransaction().rollback();
        }
      } finally {
        em.close();
      }
    }
  }

  private EventoLogistico evento(
      EventoLogistico.Tipo tipo,
      Datos... asignaciones
  ) {
    return new EventoLogistico(
        UUID.randomUUID().toString(),
        tipo,
        Arrays.stream(asignaciones)
            .map(a -> new EventoLogistico.Referencia(
                a.idDonacion(), a.idEntidad()
            ))
            .toList(),
        "ABC",
        "Motivo",
        LocalDateTime.now()
    );
  }

  private void registrar(EventoLogistico evento) {
    // Cada operación tiene su propio contexto de persistencia.
    EntityManager em = factory.createEntityManager();

    try {
      var repo = new RepositorioAsignacionesDonacion(em);
      new SeguimientoLogistico(repo, notificador).registrar(evento);
    } finally {
      try {
        if (em.getTransaction().isActive()) {
          em.getTransaction().rollback();
        }
      } finally {
        em.close();
      }
    }
  }

  private void verificarDonacion(
      Datos datos,
      EstadoDonacion estado,
      int cantidadCambios
  ) {
    EntityManager em = factory.createEntityManager();

    try {
      Donacion donacion = new RepositorioAsignacionesDonacion(em)
          .buscarPorId(datos.idAsignacion())
          .orElseThrow()
          .getDonacion();

      assertEquals(estado, donacion.getEstadoActual());
      assertEquals(cantidadCambios, donacion.getHistorialEstados().size());
    } finally {
      em.close();
    }
  }

  private void verificarEvento(
      EventoLogistico evento,
      boolean procesado,
      int cantidadPendientes
  ) {
    EntityManager em = factory.createEntityManager();

    try {
      var repo = new RepositorioAsignacionesDonacion(em);

      assertEquals(
          procesado,
          repo.buscarEventoProcesado(evento.idOperacion()).isPresent()
      );

      assertEquals(
          cantidadPendientes,
          repo.buscarNotificacionesDeEvento(evento.idOperacion()).size()
      );
    } finally {
      em.close();
    }
  }

  private void procesarPendientes() {
    new ProcesadorNotificacionesPendientes(factory, notificador)
        .ejecutar(100);
  }

  @Test
  void inicioEnLoteValidaTodosAntesDeCambiarElPrimero() {
    Datos primera = asignar(true);
    Datos segunda = asignar(false);

    EventoLogistico inicio = evento(
        EventoLogistico.Tipo.INICIO_TRASLADO,
        primera,
        segunda
    );

    assertThrows(
        IllegalStateException.class,
        () -> registrar(inicio)
    );

    verificarDonacion(
        primera, EstadoDonacion.LISTA_PARA_ENTREGAR, 2
    );
    verificarDonacion(
        segunda, EstadoDonacion.ASIGNACION_REALIZADA, 1
    );

    verificarEvento(inicio, false, 0);

    procesarPendientes();
    assertTrue(mensajes.isEmpty());
  }

  @Test
  void inicioYRecepcionNotificanAmbasPartesYNoDuplicanEnReintentos() {
    Datos datos = asignar(true);

    EventoLogistico inicio =
        evento(EventoLogistico.Tipo.INICIO_TRASLADO, datos);

    registrar(inicio);
    registrar(inicio);

    verificarDonacion(datos, EstadoDonacion.EN_TRASLADO, 3);
    verificarEvento(inicio, true, 2);

    // Registrar no envía: solamente guarda los pendientes.
    assertTrue(mensajes.isEmpty());

    procesarPendientes();
    procesarPendientes();

    assertEquals(2, mensajes.size());

    EventoLogistico recepcion =
        evento(EventoLogistico.Tipo.RECEPCION, datos);

    registrar(recepcion);
    registrar(recepcion);

    verificarDonacion(datos, EstadoDonacion.ENTREGADA, 4);
    verificarEvento(recepcion, true, 2);

    assertEquals(2, mensajes.size());

    procesarPendientes();
    procesarPendientes();

    assertEquals(4, mensajes.size());

    // Ambos comprobantes incluyen fecha y camión.
    for (String mensaje : mensajes.subList(2, 4)) {
      assertTrue(mensaje.contains("ABC"));
      assertTrue(mensaje.contains(recepcion.fecha().toString()));
    }
  }

  @Test
  void fallidaYRetornoRespetanStateYNotificaciones() {
    Datos datos = asignar(true);

    EventoLogistico inicio =
        evento(EventoLogistico.Tipo.INICIO_TRASLADO, datos);

    EventoLogistico fallida =
        evento(EventoLogistico.Tipo.NO_RECIBIDA, datos);

    registrar(inicio);
    procesarPendientes();

    registrar(fallida);

    verificarDonacion(datos, EstadoDonacion.ENTREGA_FALLIDA, 4);
    verificarEvento(fallida, true, 2);

    procesarPendientes();

    assertEquals(4, mensajes.size());
    assertTrue(
        mensajes.subList(2, 4).stream()
            .allMatch(m -> m.contains(fallida.motivo()))
    );

    EventoLogistico retorno =
        evento(EventoLogistico.Tipo.RETORNO_DEPOSITO, datos);

    registrar(retorno);
    registrar(retorno);

    verificarDonacion(datos, EstadoDonacion.EN_DEPOSITO, 5);
    verificarEvento(retorno, true, 0);

    procesarPendientes();
    assertEquals(4, mensajes.size());
  }

  @Test
  void rechazaEntidadIncorrectaYNoReutilizaIdsConOtrosDatos() {
    Datos datos = asignar(true);

    EventoLogistico inicio =
        evento(EventoLogistico.Tipo.INICIO_TRASLADO, datos);

    EventoLogistico incorrecto = new EventoLogistico(
        UUID.randomUUID().toString(),
        inicio.tipo(),
        List.of(new EventoLogistico.Referencia(
            datos.idDonacion(), "NO"
        )),
        "ABC",
        "Motivo",
        inicio.fecha()
    );

    assertThrows(
        IllegalStateException.class,
        () -> registrar(incorrecto)
    );

    verificarDonacion(datos, EstadoDonacion.LISTA_PARA_ENTREGAR, 2);
    verificarEvento(incorrecto, false, 0);

    registrar(inicio);

    EventoLogistico repetido = new EventoLogistico(
        inicio.idOperacion(),
        inicio.tipo(),
        inicio.entregas(),
        "OTRA",
        inicio.motivo(),
        inicio.fecha()
    );

    assertThrows(
        IllegalStateException.class,
        () -> registrar(repetido)
    );

    verificarDonacion(datos, EstadoDonacion.EN_TRASLADO, 3);
    verificarEvento(inicio, true, 2);

    procesarPendientes();
    procesarPendientes();

    assertEquals(2, mensajes.size());
  }
}