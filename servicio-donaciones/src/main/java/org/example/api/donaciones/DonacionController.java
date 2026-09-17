package org.example.api.donaciones;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioDonantes;
import org.example.api.donaciones.dto.CambioEstadoRequest;
import org.example.api.donaciones.dto.DonacionRequest;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donante.Donante;

import java.util.List;
import java.util.Optional;

public class DonacionController {

  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioDonantes repositorioDonantes;

  public DonacionController(RepositorioDonaciones repositorioDonaciones, RepositorioDonantes repositorioDonantes) {
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioDonantes = repositorioDonantes;
  }


  public void obtenerTodas(Context ctx) {
    List<Donacion> donaciones = repositorioDonaciones.buscarTodas();
    ctx.status(HttpStatus.OK).json(donaciones);
  }


  public void obtenerPorId(Context ctx) {
    String id = ctx.pathParam("id");
    Optional<Donacion> donacion = repositorioDonaciones.buscarPorId(id);

    if (donacion.isPresent()) {
      ctx.status(HttpStatus.OK).json(donacion.get());
    } else {
      ctx.status(HttpStatus.NOT_FOUND);
    }
  }

  // POST /donaciones
  public void crearDonacion(Context ctx) {
    DonacionRequest request = ctx.bodyAsClass(DonacionRequest.class);

    // simulacionnnnnnnnn
    Subcategoria subcategoriaMock = new Subcategoria(
        request.getIdSubcategoria(),
        "Mock",
        TipoAtributo.NO_PERECEDERO
    );

    Optional<Donante> donanteOpt = repositorioDonantes.buscarPorId(request.getIdDonante());

    if (donanteOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND)
          .result("No existe el donante: " + request.getIdDonante());
      return;
    }

    Donacion nuevaDonacion = new Donacion(
        request.getDescripcionGeneral(),
        request.getCantidad(),
        request.getUnidadMedida(),
        subcategoriaMock,
        request.getFechaVencimiento(),
        request.getEstadoBien(),
        donanteOpt.get()
    );

    repositorioDonaciones.agregar(nuevaDonacion);
    ctx.status(HttpStatus.CREATED).result("Donación creada exitosamente.");
  }

  // DELETE /donaciones/{id}
  public void eliminarDonacion(Context ctx) {
    String id = ctx.pathParam("id");
    Optional<Donacion> donacionOpt = repositorioDonaciones.buscarPorId(id);

    if (donacionOpt.isPresent()) {
      repositorioDonaciones.eliminar(donacionOpt.get());
      ctx.status(HttpStatus.OK).result("Donación eliminada exitosamente.");
    } else {
      ctx.status(HttpStatus.NOT_FOUND);
    }
  }

  // PATCH /donaciones/{id}/estado
  public void cambiarEstadoDonacion(Context ctx) {
    String id = ctx.pathParam("id");
    CambioEstadoRequest request = ctx.bodyAsClass(CambioEstadoRequest.class);

    Optional<Donacion> donacionOpt = repositorioDonaciones.buscarPorId(id);

    if (donacionOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }

    Donacion donacion = donacionOpt.get();
    donacion.cambiarEstado(request.getNuevoEstado(), request.getJustificativo());

    ctx.status(HttpStatus.OK)
        .result("Estado de la donación actualizado a: " + request.getNuevoEstado());
  }
}