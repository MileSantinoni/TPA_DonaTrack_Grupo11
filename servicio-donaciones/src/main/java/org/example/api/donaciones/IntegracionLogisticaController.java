package org.example.api.donaciones;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.List;
import java.util.Optional;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.api.donaciones.dto.AsignacionLogisticaResponse;
import org.example.dominio.donacion.AsignacionDonacion;
import java.util.Objects;
import org.example.dominio.donacion.SeguimientoLogistico;
import org.example.dominio.notificacion.Notificador;

public class IntegracionLogisticaController {
  private final RepositorioAsignacionesDonacion asignaciones;
  private final RepositorioDonaciones donaciones;
  private final SeguimientoLogistico seguimiento;

  public IntegracionLogisticaController(
      RepositorioAsignacionesDonacion asignaciones,
      RepositorioDonaciones donaciones,
      Notificador notificador
  ) {
    this.asignaciones = Objects.requireNonNull(asignaciones);
    this.donaciones = Objects.requireNonNull(donaciones);
    this.seguimiento = new SeguimientoLogistico(
        this.asignaciones,
        Objects.requireNonNull(notificador)
    );
  }

  public void registrarEvento(Context ctx) {
    var evento = ctx.bodyAsClass(org.example.dominio.donacion.EventoLogistico.class);
    try {
      seguimiento.registrar(evento);
      ctx.status(HttpStatus.NO_CONTENT);
    } catch (IllegalArgumentException e) {
      ctx.status(HttpStatus.BAD_REQUEST).result(e.getMessage());
    } catch (IllegalStateException e) {
      ctx.status(HttpStatus.CONFLICT).result(e.getMessage());
    }
  }

  public void listarAsignaciones(Context ctx) {
    List<AsignacionLogisticaResponse> respuesta = asignaciones.buscarTodas().stream()
        .map(this::convertir).toList();
    ctx.json(respuesta);
  }

  public void buscarAsignacion(Context ctx) {
    Optional<AsignacionDonacion> asignacion = asignaciones.buscarPorId(ctx.pathParam("id"));
    if (asignacion.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }
    ctx.json(convertir(asignacion.get()));
  }

  public void existeDonacion(Context ctx) {
    if (donaciones.buscarPorId(ctx.pathParam("id")).isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }
    ctx.status(HttpStatus.NO_CONTENT);
  }

  private AsignacionLogisticaResponse convertir(AsignacionDonacion asignacion) {
    var entidad = asignacion.getEntidad();
    var donacion = asignacion.getDonacion();

    return new AsignacionLogisticaResponse(
        donacion.getIdAsString(),
        entidad.getIdAsString(),
        entidad.getRazonSocial(),
        entidad.getDireccion(),
        entidad.getTelefono(),
        donacion.getEstadoActual().name(),
        entidad.getLatitud(),
        entidad.getLongitud()
    );
  }
}
