package org.example.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.javalin.http.staticfiles.Location;
import java.util.Map;
import java.util.Objects;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.server.templates.JavalinHandlebars;
import org.example.server.templates.JavalinRenderer;

public final class Server {

  private Server() {
  }

  public static Javalin crearApp() {
    String password = System.getenv("DONACIONES_DB_PASSWORD");

    if (password == null || password.isBlank()) {
      throw new IllegalStateException(
          "Falta configurar DONACIONES_DB_PASSWORD"
      );
    }

    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory(
            "simple-persistence-unit",
            Map.of("javax.persistence.jdbc.password", password)
        );

    return crearApp(factory);
  }

  // La aplicación se hace responsable de cerrar esta factory.
  public static Javalin crearApp(EntityManagerFactory factory) {
    Objects.requireNonNull(factory, "La factory es obligatoria");

    try {
      return configurarApp(factory);
    } catch (RuntimeException e) {
      cerrarFactory(factory);
      throw e;
    }
  }

  private static Javalin configurarApp(EntityManagerFactory factory) {
    ThreadLocal<EntityManager> actual = new ThreadLocal<>();

    ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule());

    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Javalin app = Javalin.create(config -> {
      config.http.maxRequestSize = 32_000_000L;

      config.jetty.multipartConfig.maxFileSize(
          10,
          io.javalin.config.SizeUnit.MB
      );

      config.jetty.multipartConfig.maxTotalRequestSize(
          30,
          io.javalin.config.SizeUnit.MB
      );
      config.jsonMapper(new JavalinJackson(mapper, false));

      config.staticFiles.add(archivos -> {
        archivos.hostedPath = "/assets";
        archivos.directory = "/assets";
        archivos.location = Location.CLASSPATH;
      });

      config.fileRenderer(
          new JavalinRenderer()
              .register("hbs", new JavalinHandlebars())
      );

      config.jetty.modifyServletContextHandler(handler -> {
        var sesiones = handler.getSessionHandler();

        sesiones.setHttpOnly(true);
        sesiones.setSameSite(
            org.eclipse.jetty.http.HttpCookie.SameSite.LAX
        );

        sesiones.getSessionCookieConfig().setSecure(
            System.getenv()
                .getOrDefault(
                    "LOGTO_REDIRECT_URI",
                    "http://localhost:8083/auth/callback"
                )
                .startsWith("https://")
        );
      });
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

    app.events(eventos -> {
      eventos.serverStopped(() -> cerrarFactory(factory));
      eventos.serverStartFailed(() -> cerrarFactory(factory));
    });

    new Router(actual::get).configure(app);

    return app;
  }

  private static void cerrarFactory(EntityManagerFactory factory) {
    if (factory.isOpen()) {
      factory.close();
    }
  }
}