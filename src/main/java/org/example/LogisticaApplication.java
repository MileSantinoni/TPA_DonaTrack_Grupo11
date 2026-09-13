package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.example.api.donaciones.AsignacionController;
import org.example.api.logistica.CamionController;
import org.example.api.logistica.RutaController;

public class LogisticaApplication {

  public static Javalin crearApp() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Javalin app = Javalin.create(config -> {
      config.jsonMapper(new JavalinJackson(objectMapper));
    });

    CamionController camionController = new CamionController();
    RutaController rutaController = new RutaController();
    AsignacionController asignacionController = new AsignacionController();

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

    return app;
  }

  public static void main(String[] args) {
    crearApp().start(8080);
  }
}