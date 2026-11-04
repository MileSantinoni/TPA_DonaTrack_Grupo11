package org.example.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Map;
import org.example.server.templates.JavalinHandlebars;
import org.example.server.templates.JavalinRenderer;

public class Server {

  private Javalin app;
  private EntityManagerFactory factory;
  private final ThreadLocal<EntityManager> entityManagerActual = new ThreadLocal<>();

  public void start(int port) {
    this.factory = initEntityManagerFactory();

    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    this.app = Javalin.create(config -> {
      config.jsonMapper(new JavalinJackson());
      initializeStaticFiles(config);
      initializeTemplating(config);
    });

    configurePersistenceMiddleware();
    configureLifecycleEvents();

    // Inicializa la configuración de rutas inyectando el proveedor de EntityManager
    new Router(entityManagerActual::get).configure(app);

    Runtime.getRuntime().addShutdownHook(new Thread(app::stop));

    app.start(port);
  }

  private EntityManagerFactory initEntityManagerFactory() {
    String password = System.getenv("DONACIONES_DB_PASSWORD");
    if (password == null || password.isBlank()) {
      throw new IllegalStateException("Falta configurar DONACIONES_DB_PASSWORD");
    }
    return Persistence.createEntityManagerFactory(
        "simple-persistence-unit",
        Map.of("javax.persistence.jdbc.password", password)
    );
  }

  private void configurePersistenceMiddleware() {
    app.before(ctx -> entityManagerActual.set(factory.createEntityManager()));

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
  }

  private void configureLifecycleEvents() {
    app.events(eventos -> {
      eventos.serverStopped(this::cerrarFactory);
      eventos.serverStartFailed(this::cerrarFactory);
    });
  }

  private void cerrarFactory() {
    if (factory != null && factory.isOpen()) {
      factory.close();
    }
  }

  private void initializeTemplating(JavalinConfig config) {
    config.fileRenderer(
        new JavalinRenderer().register("hbs", new JavalinHandlebars())
    );
  }

  private static void initializeStaticFiles(JavalinConfig config) {
    config.staticFiles.add(staticFileConfig -> {
      staticFileConfig.hostedPath = "/assets";
      staticFileConfig.directory = "/assets";
    });
  }
}