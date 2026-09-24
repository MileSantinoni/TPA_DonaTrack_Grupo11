package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import java.net.URI;
import java.net.http.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.Repositorios.RepositorioRutas;
import org.example.dominio.logistica.*;
import org.example.integracion.AsignacionDisponible;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class IntegracionDonacionesTest {

  Javalin remoto;
  Javalin app;
  EntityManagerFactory factory;

  final HttpClient http = HttpClient.newHttpClient();
  final List<EventoLogistico> eventos = new CopyOnWriteArrayList<>();
  volatile int respuesta = 204;

  @BeforeEach
  void preparar() {
    factory = Persistence.createEntityManagerFactory("logistica-test");

    remoto = Javalin.create(c -> c.jsonMapper(
        new JavalinJackson(
            new ObjectMapper().findAndRegisterModules()
        )
    ));

    remoto.get("/interno/asignaciones", ctx -> ctx.json(List.of(
        new AsignacionDisponible(
            "D1", "E1", "Comedor Sol", "Calle real",
            "123", "LISTA_PARA_ENTREGAR"
        )
    )));

    remoto.post("/interno/logistica/eventos", ctx -> {
      eventos.add(ctx.bodyAsClass(EventoLogistico.class));

      if (respuesta == 204) {
        ctx.status(204);
      } else {
        ctx.status(respuesta).result("Rechazado");
      }
    });

    remoto.start(0);

    app = LogisticaApplication.crearApp(
        "http://localhost:" + remoto.port(),
        factory
    ).start(0);
  }

  @AfterEach
  void cerrar() {
    try {
      if (app != null) {
        app.stop();
      }
    } finally {
      try {
        if (remoto != null) {
          remoto.stop();
        }
      } finally {
        if (factory != null && factory.isOpen()) {
          factory.close();
        }
      }
    }
  }

  HttpResponse<String> post(String path, String body) throws Exception {
    return http.send(
        HttpRequest.newBuilder(
                URI.create("http://localhost:" + app.port() + path)
            )
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString()
    );
  }

  void registrar() throws Exception {
    assertEquals(201, post("/camiones", """
        {
          "patente": "ABC",
          "capacidadVolumen": 30,
          "altura": 3,
          "capacidadCarga": 5000
        }
        """).statusCode());

    assertEquals(201, post("/rutas", """
        {
          "patente": "ABC",
          "latitudDeposito": -34.6,
          "longitudDeposito": -58.38,
          "entregas": [
            {"idDonacion": "D1", "orden": 1}
          ]
        }
        """).statusCode());
  }

  // Cada verificación consulta la base desde un contexto nuevo.
  // Las entidades y colecciones se leen mientras el EM está abierto.
  void verificarRuta(Consumer<Ruta> verificaciones) {
    EntityManager em = factory.createEntityManager();

    try {
      List<Ruta> encontradas =
          new RepositorioRutas(em).buscarPorPatente("ABC");

      assertEquals(1, encontradas.size());
      verificaciones.accept(encontradas.get(0));
    } finally {
      em.close();
    }
  }

  void verificarEntrega(Consumer<Entrega> verificaciones) {
    verificarRuta(ruta -> {
      assertEquals(1, ruta.cantidadEntregas());
      verificaciones.accept(ruta.getEntregas().get(0));
    });
  }

  @Test
  void registraIniciaYConfirmaPorHttpSinCompartirEntidades()
      throws Exception {

    registrar();

    verificarRuta(ruta ->
        assertFalse(ruta.getCamion().estaDisponible())
    );

    verificarEntrega(entrega -> {
      assertEquals("E1", entrega.getIdEntidad());
      assertEquals("Calle real", entrega.getDireccion());
    });

    assertEquals(
        200, post("/rutas/ABC/iniciar", "").statusCode()
    );

    verificarRuta(ruta -> {
      assertTrue(ruta.estaActiva());
      assertNotNull(ruta.getCamion().getUltimaUbicacion());
      assertEquals(
          -34.6,
          ruta.getCamion().getUltimaUbicacion().getLatitud()
      );
    });

    verificarEntrega(entrega ->
        assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstado())
    );

    assertEquals(204, post(
        "/rutas/ABC/entregas/D1/recepcion",
        "{\"fotos\":[\"foto.jpg\"]}"
    ).statusCode());

    verificarEntrega(entrega -> {
      assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstado());
      assertEquals(List.of("foto.jpg"), entrega.getFotosRecepcion());
      assertNotNull(entrega.getFechaRecepcion());
      assertNotNull(entrega.getCamionResponsable());
      assertEquals("ABC", entrega.getCamionResponsable().getPatente());
    });

    verificarRuta(ruta ->
        assertEquals(100.0, ruta.porcentajeAvance())
    );

    assertEquals(
        List.of(
            EventoLogistico.Tipo.INICIO_TRASLADO,
            EventoLogistico.Tipo.RECEPCION
        ),
        eventos.stream().map(EventoLogistico::tipo).toList()
    );
  }

  @Test
  void informaEntregaFallidaYRetornoPorHttp() throws Exception {
    registrar();

    assertEquals(
        200, post("/rutas/ABC/iniciar", "").statusCode()
    );

    assertEquals(204, post(
        "/rutas/ABC/entregas/D1/no-recibida",
        "{\"motivo\":\"Cerrado\"}"
    ).statusCode());

    verificarEntrega(entrega ->
        assertEquals(EstadoEntrega.NO_RECIBIDA, entrega.getEstado())
    );

    assertEquals(204, post(
        "/rutas/ABC/entregas/D1/retorno",
        "{\"motivo\":\"Regreso al deposito\"}"
    ).statusCode());

    verificarEntrega(entrega ->
        assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstado())
    );
  }

  @Test
  void conflictoRemotoNoCambiaElEstadoLocal() throws Exception {
    registrar();
    respuesta = 409;

    assertEquals(
        409, post("/rutas/ABC/iniciar", "").statusCode()
    );

    verificarRuta(ruta -> assertFalse(ruta.estaActiva()));

    verificarEntrega(entrega ->
        assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstado())
    );
  }

  @Test
  void errorRemotoSeReportaComo502YPermiteReintentar()
      throws Exception {

    registrar();
    respuesta = 500;

    assertEquals(
        502, post("/rutas/ABC/iniciar", "").statusCode()
    );

    verificarRuta(ruta -> assertFalse(ruta.estaActiva()));

    verificarEntrega(entrega ->
        assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstado())
    );

    respuesta = 204;

    assertEquals(
        200, post("/rutas/ABC/iniciar", "").statusCode()
    );

    assertEquals(2, eventos.size());
    assertEquals(eventos.get(0), eventos.get(1));

    verificarRuta(ruta -> assertTrue(ruta.estaActiva()));

    verificarEntrega(entrega ->
        assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstado())
    );
  }

  @Test
  void fotosInvalidasNoConfirmanRecepcion() throws Exception {
    registrar();

    assertEquals(
        200, post("/rutas/ABC/iniciar", "").statusCode()
    );

    assertEquals(400, post(
        "/rutas/ABC/entregas/D1/recepcion",
        "{\"fotos\":[\"\"]}"
    ).statusCode());

    verificarEntrega(entrega -> {
      assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstado());
      assertTrue(entrega.getFotosRecepcion().isEmpty());
    });

    assertEquals(1, eventos.size());
  }

  @Test
  void validaRutaIncompleta() throws Exception {
    assertEquals(
        400,
        post("/rutas", "{\"patente\":\"ABC\"}").statusCode()
    );

    assertEquals(
        404,
        post("/rutas/INEXISTENTE/iniciar", "").statusCode()
    );
  }
}