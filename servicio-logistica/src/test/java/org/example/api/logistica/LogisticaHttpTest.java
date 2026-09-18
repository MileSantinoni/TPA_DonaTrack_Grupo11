package org.example.api.logistica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.example.LogisticaApplication;
import org.example.Repositorios.RepositorioCamiones;
import org.example.dominio.logistica.EstadoEntrega;
import org.example.dominio.logistica.MonitorCamiones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogisticaHttpTest {
  private Javalin donaciones;
  private Javalin logistica;
  private final HttpClient http = HttpClient.newHttpClient();

  @BeforeEach
  void preparar() {
    RepositorioCamiones.getInstance().limpiar();
    MonitorCamiones.getInstance().limpiar();
    donaciones = Javalin.create();
    donaciones.get("/interno/donaciones/DON-1/existe", ctx -> ctx.status(204));
    donaciones.start(0);
    logistica = LogisticaApplication.crearApp("http://localhost:" + donaciones.port());
    logistica.start(0);
  }

  @AfterEach
  void limpiar() {
    logistica.stop();
    donaciones.stop();
    RepositorioCamiones.getInstance().limpiar();
    MonitorCamiones.getInstance().limpiar();
  }

  @Test
  void registraCamionRutaYUbicacionPorHttp() throws Exception {
    assertEquals(201, post("/camiones",
        "{\"patente\":\"AA100BB\",\"capacidadVolumen\":30,\"altura\":3,\"capacidadCarga\":5000}")
        .statusCode());
    assertEquals(201, post("/rutas",
        "{\"patente\":\"AA100BB\",\"entregas\":[{\"idDonacion\":\"DON-1\","
            + "\"razonSocial\":\"Comedor\",\"direccion\":\"Calle 1\","
            + "\"telefono\":\"111\",\"orden\":1}]}").statusCode());
    assertEquals(200, post("/rutas/AA100BB/iniciar", "").statusCode());
    assertTrue(MonitorCamiones.getInstance().rutaDe("AA100BB").estaActiva());
    assertEquals(EstadoEntrega.PENDIENTE, MonitorCamiones.getInstance()
        .rutaDe("AA100BB").getEntregas().get(0).getEstado());

    assertEquals(200, post("/camiones/ubicacion",
        "{\"patente\":\"AA100BB\",\"latitud\":-34.6037,\"longitud\":-58.3816,"
            + "\"velocidad\":55}").statusCode());
    var ubicacion = new ObjectMapper().readTree(get("/camiones/AA100BB/ubicacion").body());
    assertEquals(-34.6037, ubicacion.get("latitud").asDouble(), 0.0001);
    var avance = new ObjectMapper().readTree(get("/camiones/AA100BB/avance").body());
    assertEquals(0.0, avance.get("porcentajeAvance").asDouble(), 0.0001);
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + logistica.port() + path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String path) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + logistica.port() + path))
        .GET().build(), HttpResponse.BodyHandlers.ofString());
  }
}
