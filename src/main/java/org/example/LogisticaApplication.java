package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.api.donaciones.AsignacionController;
import org.example.api.logistica.CamionController;
import org.example.api.logistica.RutaController;

import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;
import org.example.service.NotificacionesService;
import org.example.api.donaciones.DonanteController;
import org.example.api.donaciones.DonacionController;
import org.example.api.donaciones.EntidadBeneficiariaController;
import org.example.api.donaciones.NecesidadController;

public class LogisticaApplication {

  public static Javalin crearApp() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Javalin app = Javalin.create(config -> {
      config.jsonMapper(new JavalinJackson(objectMapper));
    });

    // acá deberian instanciarse los repositorios donantes, donaciones, entidadesbeneficiarias
    // iguaaaaaaaal, ahora con persostencia esto debería editarse
    RepositorioDonantes repoDonantes = RepositorioDonantes.getInstance();
    RepositorioDonaciones repoDonaciones = RepositorioDonaciones.getInstance();
    RepositorioEntidadesBeneficiarias repoEntidades = RepositorioEntidadesBeneficiarias.getInstance();

    // también instanciar notificador y servicio
    Notificador notificador = new Notificador(
        new Email(null),
        new SMS(),
        new WhatsApp()
    );
    NotificacionesService notificacionesService = new NotificacionesService(notificador);

    //controllers de logistica
    CamionController camionController = new CamionController();
    RutaController rutaController = new RutaController();
    AsignacionController asignacionController = new AsignacionController();

    //instanciar controllers de donaciones

    DonanteController donanteController = new DonanteController(repoDonantes);
    DonacionController donacionController = new DonacionController(repoDonaciones, repoDonantes);
    EntidadBeneficiariaController entidadController = new EntidadBeneficiariaController(repoEntidades);
    NecesidadController necesidadController = new NecesidadController(repoEntidades);


    // Rutas de Logistica - Camiones
    app.post("/camiones", camionController::registrarCamion);
    app.post("/camiones/ubicacion", camionController::recibirUbicacion);
    app.get("/camiones/{patente}/ubicacion", camionController::ubicacionActual);
    app.get("/camiones/{patente}/avance", camionController::avanceDeRuta);

    // Rutas de Logistica - Rutas
    app.post("/rutas", rutaController::registrarRuta);
    app.post("/rutas/{patente}/iniciar", rutaController::iniciarRuta);

    // Rutas de Asignacion de Donaciones
    app.post("/asignaciones/ejecutar/{idDonacion}", asignacionController::ejecutarAlgoritmos);
    app.get("/asignaciones/ranking/{idDonacion}", asignacionController::obtenerRanking);
    app.post("/asignaciones/confirmar", asignacionController::confirmarAsignacion);
    app.post("/asignaciones/rechazar", asignacionController::rechazarAsignacion);

    // Rutas de donantes
    app.get("/donantes", donanteController::obtenerTodos);
    app.get("/donantes/{id}", donanteController::obtenerPorId);
    app.post("/donantes/humanas", donanteController::crearPersonaHumana);
    app.put("/donantes/humanas/{id}", donanteController::actualizarPersonaHumana);
    app.post("/donantes/juridicas", donanteController::crearPersonaJuridica);
    app.put("/donantes/juridicas/{id}", donanteController::actualizarPersonaJuridica);
    app.delete("/donantes/{id}", donanteController::eliminarDonante);

    // Rutas de donaciones
    app.get("/donaciones", donacionController::obtenerTodas);
    app.get("/donaciones/{id}", donacionController::obtenerPorId);
    app.post("/donaciones", donacionController::crearDonacion);
    app.patch("/donaciones/{id}/estado", donacionController::cambiarEstadoDonacion);
    app.delete("/donaciones/{id}", donacionController::eliminarDonacion);

    // Rutas de entidades beneficiarias
    app.get("/entidades", entidadController::obtenerTodas);
    app.get("/entidades/{id}", entidadController::obtenerPorId);
    app.post("/entidades", entidadController::crearEntidad);
    app.put("/entidades/{id}", entidadController::actualizarEntidad);
    app.delete("/entidades/{id}", entidadController::eliminarEntidad);

    // Rutas de necesidades
    app.get("/entidades/{idEntidad}/necesidades", necesidadController::obtenerNecesidades);
    app.post("/entidades/{idEntidad}/necesidades/recurrentes", necesidadController::registrarRecurrente);
    app.post("/entidades/{idEntidad}/necesidades/extraordinarias", necesidadController::registrarExtraordinaria);
    app.delete("/entidades/{idEntidad}/necesidades/{idNecesidad}", necesidadController::eliminarNecesidad);

    return app;
  }

  public static void main(String[] args) {
    crearApp().start(8080);
  }
}