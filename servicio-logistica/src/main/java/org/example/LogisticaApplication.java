package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.example.Repositorios.RepositorioCamiones;
import org.example.api.logistica.CamionController;
import org.example.api.logistica.RutaController;
import org.example.dominio.logistica.MonitorCamiones;
import org.example.integracion.ClienteDonaciones;

public class LogisticaApplication {
  public static Javalin crearApp() {
    String urlDonaciones = System.getenv().getOrDefault("DONACIONES_URL", "http://localhost:8080");
    return crearApp(urlDonaciones);
  }

  public static Javalin crearApp(String urlDonaciones) {
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper)));

    RepositorioCamiones camiones = RepositorioCamiones.getInstance();
    MonitorCamiones monitor = MonitorCamiones.getInstance();
    CamionController camionController = new CamionController(monitor, camiones);
    ClienteDonaciones clienteDonaciones = new ClienteDonaciones(urlDonaciones);
    RutaController rutaController = new RutaController(camiones, monitor, clienteDonaciones);

    app.get("/health", ctx -> ctx.result("OK"));
    org.example.Routes.RoutesLogistica.registrar(app, camionController, rutaController,
        new org.example.api.logistica.EntregaController(monitor, clienteDonaciones));
    app.exception(IllegalArgumentException.class, (e, ctx) -> ctx.status(400).result(e.getMessage()));
    app.exception(IllegalStateException.class, (e, ctx) -> ctx.status(409).result(e.getMessage()));
    app.exception(java.io.IOException.class, (e, ctx) -> ctx.status(502).result("No se pudo comunicar con Donaciones"));
    app.exception(InterruptedException.class, (e, ctx) -> {
      Thread.currentThread().interrupt();
      ctx.status(503).result("Comunicacion con Donaciones interrumpida");
    });
    return app;
  }

  public static void main(String[] args) {
    crearApp().start(8082);
  }
}
