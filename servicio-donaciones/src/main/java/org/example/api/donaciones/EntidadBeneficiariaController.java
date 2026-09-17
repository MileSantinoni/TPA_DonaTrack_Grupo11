package org.example.api.donaciones;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.api.donaciones.dto.EntidadBeneficiariaRequest;
import org.example.dominio.beneficiario.EntidadBeneficiaria;

import java.util.List;
import java.util.Optional;

public class EntidadBeneficiariaController {

  private final RepositorioEntidadesBeneficiarias repositorio;

  public EntidadBeneficiariaController(RepositorioEntidadesBeneficiarias repositorio) {
    this.repositorio = repositorio;
  }

  // GET /entidades
  public void obtenerTodas(Context ctx) {
    List<EntidadBeneficiaria> entidades = repositorio.buscarTodas();
    ctx.status(HttpStatus.OK).json(entidades);
  }

  // GET /entidades/{id}
  public void obtenerPorId(Context ctx) {
    String id = ctx.pathParam("id");
    Optional<EntidadBeneficiaria> entidad = repositorio.buscarPorId(id);

    if (entidad.isPresent()) {
      ctx.status(HttpStatus.OK).json(entidad.get());
    } else {
      ctx.status(HttpStatus.NOT_FOUND);
    }
  }

  // POST /entidades
  public void crearEntidad(Context ctx) {
    EntidadBeneficiariaRequest request = ctx.bodyAsClass(EntidadBeneficiariaRequest.class);

    EntidadBeneficiaria nuevaEntidad = new EntidadBeneficiaria(
        request.getRazonSocial(),
        request.getDireccion(),
        request.getTelefono()
    );

    repositorio.agregar(nuevaEntidad);
    ctx.status(HttpStatus.CREATED).result("Entidad Beneficiaria creada exitosamente.");
  }

  // PUT /entidades/{id}
  public void actualizarEntidad(Context ctx) {
    String id = ctx.pathParam("id");
    EntidadBeneficiariaRequest request = ctx.bodyAsClass(EntidadBeneficiariaRequest.class);

    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(id);

    if (entidadOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }

    EntidadBeneficiaria entidad = entidadOpt.get();

    entidad.setRazonSocial(request.getRazonSocial());
    entidad.setDireccion(request.getDireccion());
    entidad.setTelefono(request.getTelefono());

    ctx.status(HttpStatus.OK).result("Entidad Beneficiaria actualizada exitosamente.");
  }

  // DELETE /entidades/{id}
  public void eliminarEntidad(Context ctx) {
    String id = ctx.pathParam("id");
    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(id);

    if (entidadOpt.isPresent()) {
      repositorio.eliminar(entidadOpt.get());
      ctx.status(HttpStatus.OK).result("Entidad Beneficiaria eliminada exitosamente.");
    } else {
      ctx.status(HttpStatus.NOT_FOUND);
    }
  }
}