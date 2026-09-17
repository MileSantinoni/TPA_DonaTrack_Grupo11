package org.example.Routes;

import io.javalin.Javalin;
import org.example.api.donaciones.AsignacionController;
import org.example.api.donaciones.DonanteController;
import org.example.api.donaciones.DonacionController;
import org.example.api.donaciones.EntidadBeneficiariaController;
import org.example.api.donaciones.NecesidadController;

public final class RoutesDonaciones {

  private RoutesDonaciones() {}

  public static void registrar(Javalin app, AsignacionController asignacionController,
                               DonanteController donanteController,
                               DonacionController donacionController,
                               EntidadBeneficiariaController entidadController,
                               NecesidadController necesidadController) {
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
  }
}
