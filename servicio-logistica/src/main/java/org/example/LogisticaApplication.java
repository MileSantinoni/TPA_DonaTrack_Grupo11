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
import org.example.Repositorios.RepositorioRutas;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Map;

public class LogisticaApplication {
  public static Javalin crearApp() {
    String urlDonaciones = System.getenv().getOrDefault("DONACIONES_URL", "http://localhost:8083");
    return crearApp(urlDonaciones);
  }

  public static Javalin crearApp(String urlDonaciones) {
    String password = System.getenv("LOGISTICA_DB_PASSWORD");

    if (password == null || password.isBlank()) {
      throw new IllegalStateException(
          "Falta configurar LOGISTICA_DB_PASSWORD"
      );
    }

    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory(
            "simple-persistence-unit",
            Map.of("javax.persistence.jdbc.password", password)
        );
    try {
      return crearApp(urlDonaciones, factory);
    } catch (RuntimeException e) {
      factory.close();
      throw e;
    }
  }

  public static Javalin crearApp(
      String urlDonaciones,
      EntityManagerFactory factory
  ) {
    ThreadLocal<EntityManager> entityManagerActual = new ThreadLocal<>();
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper)));

    app.before(ctx -> {
      entityManagerActual.set(factory.createEntityManager());
    });

    app.after(ctx -> {
      EntityManager em = entityManagerActual.get();

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
        entityManagerActual.remove();
      }
    });

    app.events(eventos -> {
      eventos.serverStopped(() -> {
        if (factory.isOpen()) {
          factory.close();
        }
      });

      eventos.serverStartFailed(() -> {
        if (factory.isOpen()) {
          factory.close();
        }
      });
    });

    //REPOSITORIOS
    RepositorioCamiones camiones =
        new RepositorioCamiones(entityManagerActual::get);

    RepositorioRutas rutas =
        new RepositorioRutas(entityManagerActual::get);

    MonitorCamiones monitor = new MonitorCamiones(rutas);
    CamionController camionController = new CamionController(monitor, camiones);
    ClienteDonaciones clienteDonaciones = new ClienteDonaciones(urlDonaciones);
    RutaController rutaController = new RutaController(camiones, monitor, clienteDonaciones);

    app.get("/health", ctx -> ctx.result("OK"));
    org.example.Routes.RoutesLogistica.registrar(app, camionController, rutaController,
        new org.example.api.logistica.EntregaController(
            monitor, clienteDonaciones, rutas
        ));
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
    Javalin app = crearApp();

    Runtime.getRuntime().addShutdownHook(
        new Thread(() -> app.stop())
    );

    app.start(8082);
  }
}
