package org.example.api.logistica;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.Repositorios.RepositorioCamiones;
import org.example.api.logistica.dto.AvanceResponse;
import org.example.api.logistica.dto.CamionRequest;
import org.example.api.logistica.dto.ReporteUbicacionRequest;
import org.example.api.logistica.dto.UbicacionResponse;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.MonitorCamiones;
import org.example.dominio.logistica.ReporteUbicacion;
import org.example.dominio.logistica.UbicacionCamion;

import java.time.LocalDateTime;

public class CamionController {

  private final MonitorCamiones monitorCamiones;
  private final RepositorioCamiones repositorioCamiones;

  public CamionController(MonitorCamiones monitorCamiones, RepositorioCamiones repositorioCamiones) {
    this.monitorCamiones = monitorCamiones;
    this.repositorioCamiones = repositorioCamiones;
  }

  // Alta de un camión en la flota
  public void registrarCamion(Context ctx) {
    CamionRequest request = ctx.bodyAsClass(CamionRequest.class);
    Camion camion = new Camion(
        request.getPatente(),
        request.getCapacidadVolumen(),
        request.getAltura(),
        request.getCapacidadCarga());
    repositorioCamiones.agregar(camion);
    ctx.status(HttpStatus.CREATED).result("Camion registrado: " + camion.getPatente());
  }

  // Contrato de integración: la app móvil reporta la ubicación del camión
  public void recibirUbicacion(Context ctx) {
    ReporteUbicacionRequest request = ctx.bodyAsClass(ReporteUbicacionRequest.class);
    LocalDateTime fecha = request.getFechaYHora();
    if (fecha == null) {
      fecha = LocalDateTime.now();
    }

    ReporteUbicacion reporte = new ReporteUbicacion(
        request.getPatente(),
        request.getLatitud(),
        request.getLongitud(),
        request.getVelocidad(),
        fecha);

    boolean procesado = monitorCamiones.recibirReporte(
        reporte, repositorioCamiones
    );
    if (procesado) {
      ctx.status(HttpStatus.OK).result("Ubicacion registrada");
    } else {
      ctx.status(HttpStatus.BAD_REQUEST).result("Reporte rechazado por validacion");
    }
  }

  // Dashboard: última ubicación conocida del camión
  public void ubicacionActual(Context ctx) {
    String patente = ctx.pathParam("patente");
    UbicacionCamion ubicacion = repositorioCamiones
        .buscarPorPatente(patente)
        .map(Camion::getUltimaUbicacion)
        .orElse(null);
    if (ubicacion == null) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }

    UbicacionResponse response = new UbicacionResponse(
        ubicacion.getLatitud(),
        ubicacion.getLongitud(),
        ubicacion.getVelocidad(),
        ubicacion.getFechaYHora());
    ctx.status(HttpStatus.OK).json(response);
  }

  // Dashboard: avance del camión sobre su ruta
  public void avanceDeRuta(Context ctx) {
    String patente = ctx.pathParam("patente");
    double avance = monitorCamiones.avanceDeRuta(patente);
    ctx.status(HttpStatus.OK).json(new AvanceResponse(patente, avance));
  }
}