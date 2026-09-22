package org.example.Routes;

import io.javalin.Javalin;
import org.example.api.logistica.CamionController;
import org.example.api.logistica.RutaController;
import org.example.api.logistica.EntregaController;

public final class RoutesLogistica {
  private RoutesLogistica() {}

  public static void registrar(Javalin app, CamionController camiones,
                               RutaController rutas, EntregaController entregas) {
    app.post("/camiones", camiones::registrarCamion);
    app.post("/camiones/ubicacion", camiones::recibirUbicacion);
    app.get("/camiones/{patente}/ubicacion", camiones::ubicacionActual);
    app.get("/camiones/{patente}/avance", camiones::avanceDeRuta);
    app.post("/rutas", rutas::registrarRuta);
    app.post("/rutas/{patente}/iniciar", rutas::iniciarRuta);
    app.post("/rutas/{patente}/entregas/{idDonacion}/recepcion", entregas::confirmarRecepcion);
    app.post("/rutas/{patente}/entregas/{idDonacion}/no-recibida", entregas::marcarNoRecibida);
    app.post("/rutas/{patente}/entregas/{idDonacion}/retorno", entregas::registrarRetorno);
  }
}
