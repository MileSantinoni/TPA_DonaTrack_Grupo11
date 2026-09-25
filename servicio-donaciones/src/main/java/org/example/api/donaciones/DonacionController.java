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
import org.example.Repositorios.RepositorioCatalogo;
import java.util.UUID;
import org.example.Repositorios.RepositorioRegistroDonacion;
import org.example.dominio.catalogo.Bien;
import org.example.dominio.donacion.RegistroDonacion;

import java.util.List;
import java.util.Optional;

public class DonacionController {

  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioDonantes repositorioDonantes;
  private final RepositorioCatalogo repositorioCatalogo;
  private final RepositorioRegistroDonacion repositorioRegistros;

  public DonacionController(
      RepositorioDonaciones repositorioDonaciones,
      RepositorioDonantes repositorioDonantes,
      RepositorioCatalogo repositorioCatalogo,
      RepositorioRegistroDonacion repositorioRegistros
  ) {
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioDonantes = repositorioDonantes;
    this.repositorioCatalogo = repositorioCatalogo;
    this.repositorioRegistros = repositorioRegistros;
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

    Optional<Donante> donanteOpt =
        repositorioDonantes.buscarPorId(request.getIdDonante());

    if (donanteOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND)
          .result("No existe el donante: " + request.getIdDonante());
      return;
    }

    String idSubcategoria = request.getIdSubcategoria();

    if (idSubcategoria == null || idSubcategoria.isBlank()) {
      ctx.status(HttpStatus.BAD_REQUEST)
          .result("La subcategoria es obligatoria");
      return;
    }

    Optional<Subcategoria> subcategoriaOpt =
        repositorioCatalogo.buscarSubcategoria(idSubcategoria);

    if (subcategoriaOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND)
          .result("No existe la subcategoria: " + idSubcategoria);
      return;
    }

    Donacion nuevaDonacion = new Donacion(
        request.getDescripcionGeneral(),
        request.getCantidad(),
        request.getUnidadMedida(),
        subcategoriaOpt.get(),
        request.getFechaVencimiento(),
        request.getEstadoBien(),
        donanteOpt.get()
    );

    if (request.getDescripcionGeneral() == null
        || request.getDescripcionGeneral().isBlank()
        || request.getUnidadMedida() == null
        || request.getUnidadMedida().isBlank()
        || request.getCantidad() <= 0) {

      ctx.status(HttpStatus.BAD_REQUEST)
          .result("La descripcion y unidad son obligatorias; la cantidad debe ser positiva");
      return;
    }

    Bien bien;

    try {
      bien = new Bien(
          UUID.randomUUID().toString(),
          request.getDescripcionGeneral(),
          request.getCantidad(),
          request.getUnidadMedida(),
          subcategoriaOpt.get(),
          request.getFechaVencimiento(),
          request.getEstadoBien()
      );
    } catch (IllegalArgumentException e) {
      ctx.status(HttpStatus.BAD_REQUEST).result(e.getMessage());
      return;
    }

    RegistroDonacion registro = new RegistroDonacion(
        UUID.randomUUID().toString(),
        request.getDescripcionGeneral(),
        donanteOpt.get()
    );

    registro.agregarBien(bien);

    List<Donacion> generadas = registro.segmentar();

    repositorioRegistros.agregarRegistroConDonaciones(
        registro,
        generadas
    );

    ctx.status(HttpStatus.CREATED)
        .json(java.util.Map.of(
            "id", generadas.get(0).getIdAsString(),
            "idRegistro", registro.getId(),
            "mensaje", "Donacion creada exitosamente"
        ));
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

    repositorioDonaciones.actualizar(donacion);

    ctx.status(HttpStatus.OK)
        .result("Estado de la donación actualizado a: " + request.getNuevoEstado());
  }
}