package org.example.Routes;

import io.javalin.Javalin;
import org.example.api.logistica.CamionController;
import org.example.api.logistica.RutaController;

public final class RoutesLogistica {

  private RoutesLogistica() {}

  public static void registrar(Javalin app, CamionController camionController,
                               RutaController rutaController) {
    // Rutas de Logistica - Camiones
    app.post("/camiones", camionController::registrarCamion);
    app.post("/camiones/ubicacion", camionController::recibirUbicacion);
    app.get("/camiones/{patente}/ubicacion", camionController::ubicacionActual);
    app.get("/camiones/{patente}/avance", camionController::avanceDeRuta);

    // Rutas de Logistica - Rutas
    app.post("/rutas", rutaController::registrarRuta);
    app.post("/rutas/{patente}/iniciar", rutaController::iniciarRuta);
  }
}
