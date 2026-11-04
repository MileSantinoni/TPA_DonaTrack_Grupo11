package org.example.server;

import io.javalin.Javalin;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioCatalogo;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.Repositorios.RepositorioRegistroDonacion;
import org.example.Repositorios.RepositorioResultadosAlgoritmos;
import org.example.api.donaciones.AsignacionController;
import org.example.api.donaciones.DonacionController;
import org.example.api.donaciones.DonanteController;
import org.example.api.donaciones.EntidadBeneficiariaController;
import org.example.api.donaciones.IntegracionLogisticaController;
import org.example.api.donaciones.NecesidadController;
import org.example.controllers.LandingController;
import org.example.controllers.RegistrationController;
import org.example.dominio.algoritmo.AlgoritmoMadre;
import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;
import org.example.api.donaciones.AvisoPlanificacionController;
import org.example.api.donaciones.UbicacionEntidadController;
import org.example.integracion.ClienteGeocodificacion;

public class Router {

  private final Supplier<EntityManager> entityManagerSupplier;

  public Router(Supplier<EntityManager> entityManagerSupplier) {
    this.entityManagerSupplier = entityManagerSupplier;
  }

  public void configure(Javalin app) {
    // Repositorios
    RepositorioEntidadesBeneficiarias repoEntidades =
        new RepositorioEntidadesBeneficiarias(entityManagerSupplier);
    RepositorioDonantes repoDonantes =
        new RepositorioDonantes(entityManagerSupplier);
    RepositorioDonaciones repoDonaciones =
        new RepositorioDonaciones(entityManagerSupplier);
    RepositorioCatalogo repoCatalogo =
        new RepositorioCatalogo(entityManagerSupplier);
    RepositorioRegistroDonacion repoRegistros =
        new RepositorioRegistroDonacion(entityManagerSupplier);
    RepositorioAsignacionesDonacion repoAsignaciones =
        new RepositorioAsignacionesDonacion(entityManagerSupplier);

    UbicacionEntidadController ubicaciones =
        new UbicacionEntidadController(
            repoEntidades,
            () -> new ClienteGeocodificacion(
                System.getenv("HEIGIT_API_KEY")
            )
        );

    AvisoPlanificacionController avisoPlan =
        new AvisoPlanificacionController(
            entityManagerSupplier,
            repoAsignaciones
        );

    // Controladores API
    DonanteController donantes = new DonanteController(repoDonantes);
    DonacionController donaciones = new DonacionController(
        repoDonaciones, repoDonantes, repoCatalogo, repoRegistros
    );
    EntidadBeneficiariaController entidades =
        new EntidadBeneficiariaController(repoEntidades);
    NecesidadController necesidades =
        new NecesidadController(repoEntidades, repoCatalogo);

    Notificador notificador = new Notificador(new Email(), new SMS(), new WhatsApp());
    AsignacionController asignaciones = new AsignacionController(
        repoDonaciones,
        repoEntidades,
        RepositorioResultadosAlgoritmos.getInstance(),
        repoAsignaciones,
        new AlgoritmoMadre(),
        notificador
    );

    IntegracionLogisticaController integracion = new IntegracionLogisticaController(
        repoAsignaciones,
        repoDonaciones,
        notificador
    );

    // Controladores de vistas Handlebars
    LandingController landingController = new LandingController();
    RegistrationController registrationController = new RegistrationController(repoDonantes, repoEntidades);

    // Rutas de Vistas / Healthcheck
    app.get("/health", ctx -> ctx.result("OK"));
    app.get("/", landingController::index);

    // Maqueta del panel beneficiario (sin conexión de formularios al dominio).
    app.get("/mi-entidad", ctx -> ctx.render("miEntidad.hbs", java.util.Map.of()));

    // Rutas de Registro
    app.get("/registro", ctx -> ctx.redirect("/registro/donante"));
    app.get("/registro/donante", registrationController::registroDonante);
    app.post("/registro/donante", registrationController::crearPersonaHumana);
    app.get("/registro/donante/juridica", registrationController::registroDonanteJuridica);
    app.post("/registro/donante/juridica", registrationController::crearPersonaJuridica);
    app.get("/registro/entidad", registrationController::registroEntidad);
    app.post("/registro/entidad", registrationController::crearEntidad);
    // Rutas Donantes
    app.get("/donantes", donantes::obtenerTodos);
    app.get("/donantes/{id}", donantes::obtenerPorId);
    app.post("/donantes/humanas", donantes::crearPersonaHumana);
    app.put("/donantes/humanas/{id}", donantes::actualizarPersonaHumana);
    app.post("/donantes/juridicas", donantes::crearPersonaJuridica);
    app.put("/donantes/juridicas/{id}", donantes::actualizarPersonaJuridica);
    app.delete("/donantes/{id}", donantes::eliminarDonante);

    // Rutas Donaciones
    app.get("/donaciones", donaciones::obtenerTodas);
    app.get("/donaciones/{id}", donaciones::obtenerPorId);
    app.post("/donaciones", donaciones::crearDonacion);
    app.patch("/donaciones/{id}/estado", donaciones::cambiarEstadoDonacion);
    app.delete("/donaciones/{id}", donaciones::eliminarDonacion);
    app.post("/registros-donacion", donaciones::registrarCarga);

    // Rutas Entidades Beneficiarias y Necesidades
    app.get("/entidades", entidades::obtenerTodas);
    app.get("/entidades/{id}", entidades::obtenerPorId);
    app.post("/entidades", entidades::crearEntidad);
    app.put("/entidades/{id}", entidades::actualizarEntidad);
    app.delete("/entidades/{id}", entidades::eliminarEntidad);
    app.get("/entidades/{idEntidad}/necesidades", necesidades::obtenerNecesidades);
    app.post("/entidades/{idEntidad}/necesidades/recurrentes", necesidades::registrarRecurrente);
    app.post("/entidades/{idEntidad}/necesidades/extraordinarias", necesidades::registrarExtraordinaria);
    app.delete("/entidades/{idEntidad}/necesidades/{idNecesidad}", necesidades::eliminarNecesidad);

    // Rutas Asignaciones
    app.post("/asignaciones/ejecutar/{idDonacion}", asignaciones::ejecutarAlgoritmos);
    app.get("/asignaciones/ranking/{idDonacion}", asignaciones::obtenerRanking);
    app.post("/asignaciones/confirmar", asignaciones::confirmarAsignacion);
    app.post("/asignaciones/rechazar", asignaciones::rechazarAsignacion);

    // Rutas Integración Logística
    app.post("/interno/logistica/eventos", integracion::registrarEvento);
    app.get("/interno/asignaciones", integracion::listarAsignaciones);
    app.get("/interno/asignaciones/{id}", integracion::buscarAsignacion);
    app.get("/interno/donaciones/{id}/existe", integracion::existeDonacion);

    // Rutas
    app.get("/entidades/{id}/ubicacion/candidatos", ubicaciones::buscarCandidatos);
    app.put("/entidades/{id}/ubicacion", ubicaciones::confirmarUbicacion);
    app.post("/interno/donaciones/planificadas", avisoPlan::registrar);

    app.get("/mis-donaciones", ctx -> ctx.render("misDonaciones.hbs", java.util.Map.of()));
    app.get("/entidades-beneficiarias", ctx -> ctx.render("entidadesBeneficiarias.hbs", java.util.Map.of()));
  }
}
