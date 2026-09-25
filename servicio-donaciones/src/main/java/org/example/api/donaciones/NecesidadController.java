package org.example.api.donaciones;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.api.donaciones.dto.NecesidadExtraordinariaRequest;
import org.example.api.donaciones.dto.NecesidadRecurrenteRequest;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Necesidad;
import org.example.dominio.beneficiario.NecesidadExtraordinaria;
import org.example.dominio.beneficiario.NecesidadRecurrente;
import org.example.dominio.catalogo.Subcategoria;
import org.example.Repositorios.RepositorioCatalogo;

import java.util.List;
import java.util.Optional;

public class NecesidadController {

  private final RepositorioEntidadesBeneficiarias repositorio;
  private final RepositorioCatalogo catalogo;

  public NecesidadController(
      RepositorioEntidadesBeneficiarias repositorio,
      RepositorioCatalogo catalogo
  ) {
    this.repositorio = repositorio;
    this.catalogo = catalogo;
  }

  // GET /entidades/{idEntidad}/necesidades
  public void obtenerNecesidades(Context ctx) {
    String idEntidad = ctx.pathParam("idEntidad");
    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(idEntidad);

    if (entidadOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }

    List<Necesidad> necesidades = entidadOpt.get().getNecesidades();
    ctx.status(HttpStatus.OK).json(necesidades);
  }

  // POST /entidades/{idEntidad}/necesidades/recurrentes
  public void registrarRecurrente(Context ctx) {
    String idEntidad = ctx.pathParam("idEntidad");
    NecesidadRecurrenteRequest request = ctx.bodyAsClass(NecesidadRecurrenteRequest.class);

    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(idEntidad);

    if (entidadOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }

    EntidadBeneficiaria entidad = entidadOpt.get();

    String idSubcategoria = request.getIdSubcategoria();

    if (idSubcategoria == null || idSubcategoria.isBlank()) {
      ctx.status(HttpStatus.BAD_REQUEST)
          .result("La subcategoria es obligatoria.");
      return;
    }

    if (request.getDescripcion() == null
        || request.getDescripcion().isBlank()
        || request.getCantidadObjetivo() <= 0) {
      ctx.status(HttpStatus.BAD_REQUEST)
          .result("La descripcion es obligatoria y la cantidad debe ser positiva.");
      return;
    }

    Optional<Subcategoria> subcategoriaOpt =
        catalogo.buscarSubcategoria(idSubcategoria);

    if (subcategoriaOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND)
          .result("No existe la subcategoria: " + idSubcategoria);
      return;
    }

    Subcategoria subcategoria = subcategoriaOpt.get();


    if (request.getFechaInicioPeriodo() == null
        || request.getPeriodicidad() == null) {
      ctx.status(HttpStatus.BAD_REQUEST)
          .result("La fecha de inicio y la periodicidad son obligatorias.");
      return;
    }

    NecesidadRecurrente nuevaNecesidad = new NecesidadRecurrente(
        request.getDescripcion(),
        request.getCantidadObjetivo(),
        subcategoria,
        request.getFechaInicioPeriodo(),
        request.getPeriodicidad()
    );

    entidad.registrarNecesidad(nuevaNecesidad);
    repositorio.actualizar(entidad);
    ctx.status(HttpStatus.CREATED).result("Necesidad recurrente registrada exitosamente.");
  }

  // POST /entidades/{idEntidad}/necesidades/extraordinarias
  public void registrarExtraordinaria(Context ctx) {
    String idEntidad = ctx.pathParam("idEntidad");
    NecesidadExtraordinariaRequest request = ctx.bodyAsClass(NecesidadExtraordinariaRequest.class);

    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(idEntidad);

    if (entidadOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }

    EntidadBeneficiaria entidad = entidadOpt.get();

    String idSubcategoria = request.getIdSubcategoria();

    if (idSubcategoria == null || idSubcategoria.isBlank()) {
      ctx.status(HttpStatus.BAD_REQUEST)
          .result("La subcategoria es obligatoria.");
      return;
    }

    if (request.getDescripcion() == null
        || request.getDescripcion().isBlank()
        || request.getCantidadObjetivo() <= 0) {
      ctx.status(HttpStatus.BAD_REQUEST)
          .result("La descripcion es obligatoria y la cantidad debe ser positiva.");
      return;
    }

    Optional<Subcategoria> subcategoriaOpt =
        catalogo.buscarSubcategoria(idSubcategoria);

    if (subcategoriaOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND)
          .result("No existe la subcategoria: " + idSubcategoria);
      return;
    }

    Subcategoria subcategoria = subcategoriaOpt.get();


    if (request.getMotivo() == null || request.getMotivo().isBlank()) {
      ctx.status(HttpStatus.BAD_REQUEST)
          .result("El motivo es obligatorio.");
      return;
    }

    NecesidadExtraordinaria nuevaNecesidad = new NecesidadExtraordinaria(
        request.getDescripcion(),
        request.getCantidadObjetivo(),
        subcategoria,
        request.getMotivo()
    );



    entidad.registrarNecesidad(nuevaNecesidad);
    repositorio.actualizar(entidad);
    ctx.status(HttpStatus.CREATED).result("Necesidad extraordinaria registrada exitosamente.");
  }

  // DELETE /entidades/{idEntidad}/necesidades/{idNecesidad}
  public void eliminarNecesidad(Context ctx) {
    String idEntidad = ctx.pathParam("idEntidad");

    Long idNecesidad;
    try {
      idNecesidad = Long.valueOf(ctx.pathParam("idNecesidad"));
    } catch (NumberFormatException e) {
      ctx.status(HttpStatus.BAD_REQUEST)
          .result("El ID de necesidad debe ser numerico.");
      return;
    }

    Optional<EntidadBeneficiaria> entidadOpt =
        repositorio.buscarPorId(idEntidad);

    if (entidadOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND)
          .result("No existe la entidad beneficiaria.");
      return;
    }

    EntidadBeneficiaria entidad = entidadOpt.get();

    boolean removida = entidad.getNecesidades()
        .removeIf(n -> idNecesidad.equals(n.getId()));

    if (!removida) {
      ctx.status(HttpStatus.NOT_FOUND)
          .result("Necesidad no encontrada en esta entidad.");
      return;
    }

    repositorio.actualizar(entidad);

    ctx.status(HttpStatus.OK).result("Necesidad eliminada.");
  }
}