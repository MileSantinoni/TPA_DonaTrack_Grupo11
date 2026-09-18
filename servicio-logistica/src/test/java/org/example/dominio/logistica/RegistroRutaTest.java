package org.example.dominio.logistica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.example.LogisticaApplication;
import org.example.Repositorios.RepositorioCamiones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistroRutaTest {
  private Javalin donaciones;
  private Javalin logistica;
  private final RepositorioCamiones camiones = RepositorioCamiones.getInstance();
  private final MonitorCamiones monitor = MonitorCamiones.getInstance();

  @BeforeEach
  void preparar() {
    camiones.limpiar();
    monitor.limpiar();
    donaciones = Javalin.create();
    donaciones.get("/interno/donaciones/{id}/existe", ctx -> {
      ctx.status(ctx.pathParam("id").equals("DON-1") ? 204 : 404);
    });
    donaciones.start(0);
    logistica = LogisticaApplication.crearApp("http://localhost:" + donaciones.port());
    logistica.start(0);
  }

  @AfterEach
  void limpiar() {
    logistica.stop();
    donaciones.stop();
    camiones.limpiar();
    monitor.limpiar();
  }

  @Test
  void registrarRutaDejaElCamionNoDisponible() throws Exception {
    Camion camion = new Camion("AB123CD", 20, 3, 3500);
    camiones.agregar(camion);
    assertEquals(201, registrar("DON-1"));
    assertFalse(camion.estaDisponible());
    assertEquals(1, monitor.rutaDe("AB123CD").cantidadEntregas());
  }

  @Test
  void noRegistraRutaSiElCamionNoEstaDisponible() throws Exception {
    Camion camion = new Camion("AB123CD", 20, 3, 3500);
    camion.marcarNoDisponible();
    camiones.agregar(camion);
    assertEquals(404, registrar("DON-1"));
    assertNull(monitor.rutaDe("AB123CD"));
  }

  @Test
  void noRegistraRutaSiLaDonacionNoExiste() throws Exception {
    Camion camion = new Camion("AB123CD", 20, 3, 3500);
    camiones.agregar(camion);
    assertEquals(404, registrar("DON-DESCONOCIDA"));
    assertNull(monitor.rutaDe("AB123CD"));
  }

  private int registrar(String idDonacion) throws Exception {
    String cuerpo = "{\"patente\":\"AB123CD\",\"entregas\":[{\"idDonacion\":\""
        + idDonacion + "\",\"razonSocial\":\"Comedor\",\"direccion\":\"Calle 1\","
        + "\"telefono\":\"111\",\"orden\":1}]}";
    HttpRequest pedido = HttpRequest.newBuilder(
        URI.create("http://localhost:" + logistica.port() + "/rutas"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(cuerpo)).build();
    return HttpClient.newHttpClient().send(pedido, HttpResponse.BodyHandlers.discarding()).statusCode();
  }
}
