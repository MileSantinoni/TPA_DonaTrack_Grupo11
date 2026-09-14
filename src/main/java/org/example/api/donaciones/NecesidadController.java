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
import org.example.dominio.catalogo.TipoAtributo;

import java.util.List;
import java.util.Optional;

public class NecesidadController {

  private final RepositorioEntidadesBeneficiarias repositorio;

  public NecesidadController(RepositorioEntidadesBeneficiarias repositorio) {
    this.repositorio = repositorio;
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

    // mock temporal
    // TODO
    Subcategoria subcategoriaMock = new Subcategoria(
        request.getIdSubcategoria(),
        "Mock",
        TipoAtributo.NO_PERECEDERO
    );

    NecesidadRecurrente nuevaNecesidad = new NecesidadRecurrente(
        request.getDescripcion(),
        request.getCantidadObjetivo(),
        subcategoriaMock,
        request.getFechaInicioPeriodo(),
        request.getPeriodicidad()
    );

    entidad.registrarNecesidad(nuevaNecesidad);
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

    Subcategoria subcategoriaMock = new Subcategoria(
        request.getIdSubcategoria(),
        "Mock",
        TipoAtributo.NO_PERECEDERO
    );

    NecesidadExtraordinaria nuevaNecesidad = new NecesidadExtraordinaria(
        request.getDescripcion(),
        request.getCantidadObjetivo(),
        subcategoriaMock,
        request.getMotivo()
    );

    entidad.registrarNecesidad(nuevaNecesidad);
    ctx.status(HttpStatus.CREATED).result("Necesidad extraordinaria registrada exitosamente.");
  }

  // DELETE /entidades/{idEntidad}/necesidades/{idNecesidad}
  public void eliminarNecesidad(Context ctx) {
    String idEntidad = ctx.pathParam("idEntidad");
    String idNecesidad = ctx.pathParam("idNecesidad");

    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(idEntidad);

    if (entidadOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }

    EntidadBeneficiaria entidad = entidadOpt.get();

    boolean removida = entidad.getNecesidades()
        .removeIf(n -> n.getId().equals(idNecesidad));

    if (removida) {
      ctx.status(HttpStatus.OK).result("Necesidad eliminada.");
    } else {
      ctx.status(HttpStatus.NOT_FOUND)
          .result("Necesidad no encontrada en esta entidad.");
    }
  }
}