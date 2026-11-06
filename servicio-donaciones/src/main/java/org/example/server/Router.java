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
import org.example.controllers.MisDonacionesController;
import org.example.controllers.EntidadesBeneficiariasController;

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
        new AlgoritmoMadre(
            new org.example.dominio.algoritmo.AlgoritmoCompatibilidadSemantica(),
            new org.example.dominio.algoritmo.AlgoritmoPrioridadSubAtendidos(
                repoAsignaciones
            )
        ),
        notificador
    );

    IntegracionLogisticaController integracion = new IntegracionLogisticaController(
        repoAsignaciones,
        repoDonaciones,
        notificador
    );

    // Controladores de vistas Handlebars
    LandingController landingController = new LandingController(repoEntidades, repoDonaciones, repoAsignaciones);
    MisDonacionesController misDonaciones = new MisDonacionesController(entityManagerSupplier);
    EntidadesBeneficiariasController entidadesBeneficiarias = new EntidadesBeneficiariasController(entityManagerSupplier, misDonaciones);
    var adminDonaciones = new org.example.controllers.AdminDonacionesController(entityManagerSupplier);
    var adminDonantes = new org.example.controllers.AdminDonantesController(entityManagerSupplier);
    var adminCamiones = new org.example.controllers.AdminCamionesController();

    var miEntidad = new org.example.controllers.MiEntidadController(entityManagerSupplier);
    var recepcionesEntidad = new org.example.controllers.MiEntidadRecepcionController(miEntidad);

    var planificacionAdmin = new org.example.controllers.AdminPlanificacionController(entityManagerSupplier);



    // Rutas de Vistas / Healthcheck
    app.get("/administracion", ctx -> ctx.render("administracion.hbs", java.util.Map.of()));
    app.get("/health", ctx -> ctx.result("OK"));
    app.get("/", landingController::index);
    app.get("/mis-donaciones", misDonaciones::index);
    app.get("/mis-donaciones/{id}/seguimiento", misDonaciones::seguimiento);
    app.get("/entidades-beneficiarias", entidadesBeneficiarias::index);
    app.get("/administracion/donaciones", adminDonaciones::index);
    app.get("/administracion/donaciones/buscar-donantes", adminDonaciones::buscarDonantes);
    app.get("/administracion/donaciones/{id}/propuestas", adminDonaciones::propuestas);
    app.get("/administracion/donantes", adminDonantes::index);
    app.get("/administracion/donantes/{id}", adminDonantes::detalle);
    app.post("/administracion/donantes", adminDonantes::guardar);
    app.post("/administracion/donantes/{id}/editar", adminDonantes::guardar);
    app.post("/administracion/donantes/{id}/eliminar", adminDonantes::eliminar);
    app.post("/administracion/donantes/importar", adminDonantes::importar);
    app.get("/administracion/camiones", adminCamiones::index);
    app.post("/administracion/camiones", adminCamiones::guardar);
    app.post("/administracion/camiones/{patente}/editar", adminCamiones::guardar);
    app.post("/administracion/camiones/{patente}/disponibilidad", adminCamiones::cambiarDisponibilidad);
    app.post("/administracion/camiones/{patente}/eliminar", adminCamiones::eliminar);
    app.get("/administracion/planificaciones/pendientes", planificacionAdmin::pendientes);
    app.post("/administracion/planificaciones", planificacionAdmin::planificar);
    app.post("/administracion/rutas/{idRuta}/iniciar", planificacionAdmin::iniciar);

    app.get("/mi-entidad", miEntidad::index);
    app.get("/mi-entidad/necesidades/{id}", miEntidad::detalleNecesidad);
    app.post("/mi-entidad/necesidades", miEntidad::guardarNecesidad);
    app.post("/mi-entidad/necesidades/{id}/editar", miEntidad::guardarNecesidad);
    app.post("/mi-entidad/necesidades/{id}/eliminar", miEntidad::eliminarNecesidad);
    app.get("/mi-entidad/donaciones/{id}/seguimiento", miEntidad::seguimiento);
    app.post("/mi-entidad/donaciones/{id}/recepcion", recepcionesEntidad::confirmar);
    app.get("/mi-entidad/fotos/{id}/{archivo}", recepcionesEntidad::foto);

    // Las cuentas se crean primero en Logto.
    app.get("/registro", ctx -> ctx.redirect("/auth/registro"));
    app.get("/registro/donante", ctx -> ctx.redirect("/auth/registro"));
    app.get("/registro/donante/juridica", ctx -> ctx.redirect("/auth/registro"));
    app.get("/registro/entidad", ctx -> ctx.redirect("/auth/registro"));

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


    // Autenticación y perfil local
    var logto = new org.example.controllers.LogtoController();
    var cuentas = new org.example.controllers.CuentaController(
        entityManagerSupplier,
        landingController
    );

    org.example.seguridad.SesionWeb.configurar(
        app,
        entityManagerSupplier
    );

    app.get("/login", logto::login);
    app.get("/auth/registro", logto::registro);
    app.get("/auth/callback", logto::callback);
    app.get("/auth/me", logto::actual);
    app.get("/logout", logto::confirmarSalida);
    app.post("/auth/logout", logto::logout);

    app.get("/cuenta/continuar", cuentas::continuar);
    app.get("/cuenta/completar", cuentas::formulario);
    app.post("/cuenta/completar", cuentas::guardar);

  }
}
