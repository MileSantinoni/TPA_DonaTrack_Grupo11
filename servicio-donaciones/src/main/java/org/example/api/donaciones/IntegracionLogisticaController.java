package org.example.api.donaciones;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.List;
import java.util.Optional;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.api.donaciones.dto.AsignacionLogisticaResponse;
import org.example.dominio.donacion.AsignacionDonacion;

public class IntegracionLogisticaController {
  private final RepositorioAsignacionesDonacion asignaciones = RepositorioAsignacionesDonacion.getInstance();
  private final RepositorioDonaciones donaciones = RepositorioDonaciones.getInstance();

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
    return new AsignacionLogisticaResponse(donacion.getId(), entidad.getId(),
        entidad.getRazonSocial(), entidad.getDireccion(), entidad.getTelefono(),
        donacion.getEstadoActual().name());
  }
}
