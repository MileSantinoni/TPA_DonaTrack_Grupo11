package org.example.api.logistica;

import io.javalin.http.Context;
import org.example.Repositorios.RepositorioCamiones;
import org.example.api.logistica.dto.RutaRequest;
import org.example.dominio.logistica.Donaciones;
import org.example.dominio.logistica.MonitorCamiones;

public class RutaController {
  private final RepositorioCamiones camiones;
  private final MonitorCamiones monitor;
  private final Donaciones donaciones;

  public RutaController(RepositorioCamiones camiones, MonitorCamiones monitor, Donaciones donaciones) {
    this.camiones = camiones;
    this.monitor = monitor;
    this.donaciones = donaciones;
  }

  public void registrarRuta(Context ctx) throws java.io.IOException, InterruptedException {
    RutaRequest request = ctx.bodyAsClass(RutaRequest.class);
    if (monitor.registrarRuta(request.aPlan(), camiones, donaciones)) {
      ctx.status(201).result("Ruta registrada");
    } else {
      ctx.status(404).result("No se encontro camion disponible o asignacion para la donacion");
    }
  }

  public void iniciarRuta(Context ctx) throws java.io.IOException, InterruptedException {
    if (monitor.iniciarRuta(ctx.pathParam("patente"), donaciones, camiones)) {
      ctx.status(200).result("Ruta iniciada");
    } else {
      ctx.status(404).result("No hay ruta para el camion");
    }
  }
}
