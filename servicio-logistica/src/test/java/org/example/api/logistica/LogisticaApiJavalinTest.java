package org.example.api.logistica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.example.LogisticaApplication;
import org.example.Repositorios.RepositorioCamiones;
import org.example.dominio.logistica.MonitorCamiones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LogisticaApiJavalinTest {
  private Javalin app;
  private final HttpClient http = HttpClient.newHttpClient();

  @BeforeEach
  void preparar() {
    RepositorioCamiones.getInstance().limpiar();
    MonitorCamiones.getInstance().limpiar();
    app = LogisticaApplication.crearApp("http://localhost:1").start(0);
  }

  @AfterEach
  void limpiar() {
    app.stop();
    RepositorioCamiones.getInstance().limpiar();
    MonitorCamiones.getInstance().limpiar();
  }

  @Test
  void registraCamion() throws Exception {
    assertEquals(201, post("/camiones",
        "{\"patente\":\"AB123CD\",\"capacidadVolumen\":25,\"altura\":2.8,"
            + "\"capacidadCarga\":4000}").statusCode());
    assertFalse(RepositorioCamiones.getInstance().buscarPorPatente("AB123CD").isEmpty());
  }

  @Test
  void rechazaUbicacionSiNoHayRutaActiva() throws Exception {
    assertEquals(400, post("/camiones/ubicacion",
        "{\"patente\":\"AB123CD\",\"latitud\":-34.6,\"longitud\":-58.38,"
            + "\"velocidad\":50}").statusCode());
  }

  @Test
  void devuelveNoEncontradoParaRutaYUbicacionInexistentes() throws Exception {
    assertEquals(404, post("/rutas/AB123CD/iniciar", "").statusCode());
    assertEquals(404, get("/camiones/AB123CD/ubicacion").statusCode());
  }

  private HttpResponse<String> post(String path, String body) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
        HttpResponse.BodyHandlers.ofString());
  }

  private HttpResponse<String> get(String path) throws Exception {
    return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path))
        .GET().build(), HttpResponse.BodyHandlers.ofString());
  }
}
