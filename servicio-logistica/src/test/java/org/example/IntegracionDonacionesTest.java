package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.example.Repositorios.RepositorioCamiones;
import org.example.dominio.logistica.MonitorCamiones;
import org.example.dominio.logistica.EstadoEntrega;
import org.junit.jupiter.api.Test;

public class IntegracionDonacionesTest {
  @Test
  void registraEIniciaRutaSinCompartirObjetosDelDominioDonaciones() throws Exception {
    Javalin donaciones = Javalin.create();
    donaciones.get("/interno/donaciones/DON-1/existe", ctx -> ctx.status(204));
    donaciones.start(0);

    RepositorioCamiones.getInstance().limpiar();
    MonitorCamiones.getInstance().limpiar();
    Javalin logistica = LogisticaApplication.crearApp("http://localhost:" + donaciones.port());
    logistica.start(0);
    try {
      HttpClient http = HttpClient.newHttpClient();
      String base = "http://localhost:" + logistica.port();
      HttpResponse<String> camion = http.send(HttpRequest.newBuilder(URI.create(base + "/camiones"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(
              "{\"patente\":\"AA100BB\",\"capacidadVolumen\":30,\"altura\":3,\"capacidadCarga\":5000}"))
          .build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(201, camion.statusCode());

      HttpResponse<String> ruta = http.send(HttpRequest.newBuilder(URI.create(base + "/rutas"))
          .header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(
              "{\"patente\":\"AA100BB\",\"entregas\":[{\"idDonacion\":\"DON-1\","
                  + "\"razonSocial\":\"Comedor Sol\",\"direccion\":\"Av. Central 10\","
                  + "\"telefono\":\"1122334455\",\"orden\":1}]}"))
          .build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(201, ruta.statusCode(), ruta.body());
      assertNotNull(MonitorCamiones.getInstance().rutaDe("AA100BB"));
      assertEquals("Comedor Sol", MonitorCamiones.getInstance().rutaDe("AA100BB")
          .getEntregas().get(0).getRazonSocial());
      assertEquals(EstadoEntrega.PENDIENTE, MonitorCamiones.getInstance().rutaDe("AA100BB")
          .getEntregas().get(0).getEstado());

      HttpResponse<String> inicio = http.send(HttpRequest.newBuilder(
          URI.create(base + "/rutas/AA100BB/iniciar"))
          .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(200, inicio.statusCode(), inicio.body());
      assertTrue(MonitorCamiones.getInstance().rutaDe("AA100BB").estaActiva());
      assertEquals(EstadoEntrega.PENDIENTE, MonitorCamiones.getInstance().rutaDe("AA100BB")
          .getEntregas().get(0).getEstado());
    } finally {
      logistica.stop();
      donaciones.stop();
      RepositorioCamiones.getInstance().limpiar();
      MonitorCamiones.getInstance().limpiar();
    }
  }
}
