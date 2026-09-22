package org.example;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.example.Repositorios.RepositorioCamiones;
import org.example.dominio.logistica.*;
import org.example.integracion.AsignacionDisponible;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class IntegracionDonacionesTest {
  Javalin remoto;
  Javalin app;
  HttpClient http = HttpClient.newHttpClient();
  List<EventoLogistico> eventos = new ArrayList<>();
  int respuesta = 204;

  @BeforeEach void preparar() {
    RepositorioCamiones.getInstance().limpiar();
    MonitorCamiones.getInstance().limpiar();
    remoto = Javalin.create(c -> c.jsonMapper(new JavalinJackson(new ObjectMapper().findAndRegisterModules())));
    remoto.get("/interno/asignaciones", ctx -> ctx.json(List.of(
        new AsignacionDisponible("D1", "E1", "Comedor Sol", "Calle real", "123", "LISTA_PARA_ENTREGAR"))));
    remoto.post("/interno/logistica/eventos", ctx -> {
      eventos.add(ctx.bodyAsClass(EventoLogistico.class));
      ctx.status(respuesta).result(respuesta == 204 ? "" : "Rechazado");
    });
    remoto.start(0);
    app = LogisticaApplication.crearApp("http://localhost:" + remoto.port()).start(0);
  }
  @AfterEach void cerrar() {
    app.stop(); remoto.stop();
    RepositorioCamiones.getInstance().limpiar(); MonitorCamiones.getInstance().limpiar();
  }
  HttpResponse<String> post(String path, String body) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path))
        .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString());
  }
  void registrar() throws Exception {
    assertEquals(201, post("/camiones", "{\"patente\":\"ABC\",\"capacidadVolumen\":30,\"altura\":3,\"capacidadCarga\":5000}").statusCode());
    assertEquals(201, post("/rutas", "{\"patente\":\"ABC\",\"latitudDeposito\":-34.6,\"longitudDeposito\":-58.38,\"entregas\":[{\"idDonacion\":\"D1\",\"orden\":1}]}").statusCode());
  }
  Entrega entrega() { return MonitorCamiones.getInstance().rutaDe("ABC").getEntregas().get(0); }

  @Test void registraIniciaYConfirmaPorHttpSinCompartirEntidades() throws Exception {
    registrar();
    assertEquals("E1", entrega().getIdEntidad());
    assertEquals("Calle real", entrega().getDireccion());
    assertEquals(200, post("/rutas/ABC/iniciar", "").statusCode());
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega().getEstado());
    assertEquals(-34.6, MonitorCamiones.getInstance().ubicacionActual("ABC").getLatitud());
    assertEquals(204, post("/rutas/ABC/entregas/D1/recepcion", "{\"fotos\":[\"foto.jpg\"]}").statusCode());
    assertEquals(EstadoEntrega.ENTREGADA, entrega().getEstado());
    assertEquals(List.of("foto.jpg"), entrega().getFotosRecepcion());
    assertEquals(100, MonitorCamiones.getInstance().avanceDeRuta("ABC"));
    assertEquals(List.of(EventoLogistico.Tipo.INICIO_TRASLADO, EventoLogistico.Tipo.RECEPCION),
        eventos.stream().map(EventoLogistico::tipo).toList());
  }
  @Test void informaEntregaFallidaYRetornoPorHttp() throws Exception {
    registrar(); post("/rutas/ABC/iniciar", "");
    assertEquals(204, post("/rutas/ABC/entregas/D1/no-recibida", "{\"motivo\":\"Cerrado\"}").statusCode());
    assertEquals(EstadoEntrega.NO_RECIBIDA, entrega().getEstado());
    assertEquals(204, post("/rutas/ABC/entregas/D1/retorno", "{\"motivo\":\"Regreso al deposito\"}").statusCode());
    assertEquals(EstadoEntrega.PENDIENTE, entrega().getEstado());
  }
  @Test void conflictoRemotoNoCambiaElEstadoLocal() throws Exception {
    registrar(); respuesta = 409;
    assertEquals(409, post("/rutas/ABC/iniciar", "").statusCode());
    assertFalse(MonitorCamiones.getInstance().rutaDe("ABC").estaActiva());
    assertEquals(EstadoEntrega.PENDIENTE, entrega().getEstado());
  }
  @Test void errorRemotoSeReportaComo502YPermiteReintentar() throws Exception {
    registrar(); respuesta = 500;
    assertEquals(502, post("/rutas/ABC/iniciar", "").statusCode());
    respuesta = 204;
    assertEquals(200, post("/rutas/ABC/iniciar", "").statusCode());
    assertEquals(eventos.get(0), eventos.get(1));
  }
  @Test void fotosInvalidasNoConfirmanRecepcion() throws Exception {
    registrar(); post("/rutas/ABC/iniciar", "");
    assertEquals(400, post("/rutas/ABC/entregas/D1/recepcion", "{\"fotos\":[\"\"]}").statusCode());
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega().getEstado());
    assertEquals(1, eventos.size());
  }
  @Test void validaRutaIncompleta() throws Exception {
    assertEquals(400, post("/rutas", "{\"patente\":\"ABC\"}").statusCode());
    assertEquals(404, post("/rutas/INEXISTENTE/iniciar", "").statusCode());
  }
}
