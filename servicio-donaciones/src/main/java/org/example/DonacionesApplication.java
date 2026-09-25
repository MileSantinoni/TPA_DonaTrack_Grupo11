package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.example.Repositorios.RepositorioCatalogo;
import org.example.api.donaciones.AsignacionController;
import org.example.api.donaciones.DonacionController;
import org.example.api.donaciones.DonanteController;
import org.example.api.donaciones.EntidadBeneficiariaController;
import org.example.api.donaciones.IntegracionLogisticaController;
import org.example.api.donaciones.NecesidadController;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioResultadosAlgoritmos;
import org.example.dominio.algoritmo.AlgoritmoMadre;
import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;

public class DonacionesApplication {
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

    try {
      return crearApp(factory);
    } catch (RuntimeException e) {
      factory.close();
      throw e;
    }
  }

  private static void cerrarFactory(EntityManagerFactory factory) {
    if (factory.isOpen()) {
      factory.close();
    }
  }

  public static Javalin crearApp(EntityManagerFactory factory) {
    ThreadLocal<EntityManager> entityManagerActual = new ThreadLocal<>();
    // Debajo continúa tu código de ObjectMapper y Javalin.create.
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
      eventos.serverStopped(() -> cerrarFactory(factory));
      eventos.serverStartFailed(() -> cerrarFactory(factory));
    });

    RepositorioEntidadesBeneficiarias repoEntidades =
        new RepositorioEntidadesBeneficiarias(entityManagerActual::get);

    RepositorioDonantes repoDonantes =
        new RepositorioDonantes(entityManagerActual::get);

    RepositorioDonaciones repoDonaciones =
        new RepositorioDonaciones(entityManagerActual::get);

    RepositorioCatalogo repoCatalogo =
        new RepositorioCatalogo(entityManagerActual::get);

    DonanteController donantes =
        new DonanteController(repoDonantes);

    DonacionController donaciones = new DonacionController(
        repoDonaciones,
        repoDonantes,
        repoCatalogo
    );

    EntidadBeneficiariaController entidades =
        new EntidadBeneficiariaController(repoEntidades);

    NecesidadController necesidades =
        new NecesidadController(repoEntidades);

    AsignacionController asignaciones = new AsignacionController(
        repoDonaciones,
        repoEntidades,
        RepositorioResultadosAlgoritmos.getInstance(),
        RepositorioAsignacionesDonacion.getInstance(),
        new AlgoritmoMadre(),
        new Notificador(new Email(), new SMS(), new WhatsApp())
    );

    IntegracionLogisticaController integracion =
        new IntegracionLogisticaController();

    app.get("/health", ctx -> ctx.result("OK"));

    app.get("/donantes", donantes::obtenerTodos);
    app.get("/donantes/{id}", donantes::obtenerPorId);
    app.post("/donantes/humanas", donantes::crearPersonaHumana);
    app.put("/donantes/humanas/{id}", donantes::actualizarPersonaHumana);
    app.post("/donantes/juridicas", donantes::crearPersonaJuridica);
    app.put("/donantes/juridicas/{id}", donantes::actualizarPersonaJuridica);
    app.delete("/donantes/{id}", donantes::eliminarDonante);

    app.get("/donaciones", donaciones::obtenerTodas);
    app.get("/donaciones/{id}", donaciones::obtenerPorId);
    app.post("/donaciones", donaciones::crearDonacion);
    app.patch("/donaciones/{id}/estado", donaciones::cambiarEstadoDonacion);
    app.delete("/donaciones/{id}", donaciones::eliminarDonacion);

    app.get("/entidades", entidades::obtenerTodas);
    app.get("/entidades/{id}", entidades::obtenerPorId);
    app.post("/entidades", entidades::crearEntidad);
    app.put("/entidades/{id}", entidades::actualizarEntidad);
    app.delete("/entidades/{id}", entidades::eliminarEntidad);
    app.get("/entidades/{idEntidad}/necesidades", necesidades::obtenerNecesidades);
    app.post("/entidades/{idEntidad}/necesidades/recurrentes", necesidades::registrarRecurrente);
    app.post("/entidades/{idEntidad}/necesidades/extraordinarias", necesidades::registrarExtraordinaria);
    app.delete("/entidades/{idEntidad}/necesidades/{idNecesidad}", necesidades::eliminarNecesidad);

    app.post("/asignaciones/ejecutar/{idDonacion}", asignaciones::ejecutarAlgoritmos);
    app.get("/asignaciones/ranking/{idDonacion}", asignaciones::obtenerRanking);
    app.post("/asignaciones/confirmar", asignaciones::confirmarAsignacion);
    app.post("/asignaciones/rechazar", asignaciones::rechazarAsignacion);

    app.post("/interno/logistica/eventos", integracion::registrarEvento);
    app.get("/interno/asignaciones", integracion::listarAsignaciones);
    app.get("/interno/asignaciones/{id}", integracion::buscarAsignacion);
    app.get("/interno/donaciones/{id}/existe", integracion::existeDonacion);
    return app;
  }

  public static void main(String[] args) {
    Javalin app = crearApp();

    Runtime.getRuntime().addShutdownHook(
        new Thread(app::stop)
    );

    app.start(8083);
  }
}