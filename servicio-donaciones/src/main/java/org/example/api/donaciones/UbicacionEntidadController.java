package org.example.api.donaciones;

import io.javalin.http.Context;
import java.io.IOException;
import java.util.function.Supplier;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.integracion.ClienteGeocodificacion;

public class UbicacionEntidadController {

  private final RepositorioEntidadesBeneficiarias repositorio;
  private final Supplier<ClienteGeocodificacion> geocodificador;

  public UbicacionEntidadController(
      RepositorioEntidadesBeneficiarias repositorio,
      Supplier<ClienteGeocodificacion> geocodificador
  ) {
    this.repositorio = repositorio;
    this.geocodificador = geocodificador;
  }

  public void buscarCandidatos(Context ctx) {
    var encontrada = repositorio.buscarPorId(ctx.pathParam("id"));

    if (encontrada.isEmpty()) {
      ctx.status(404).result("Entidad no encontrada");
      return;
    }

    String direccion = encontrada.get().getDireccion();

    if (direccion == null || direccion.isBlank()) {
      ctx.status(400).result("La entidad no tiene direccion");
      return;
    }

    try {
      var candidatos = geocodificador.get().buscar(direccion);

      ctx.json(new ResultadoBusqueda(direccion, candidatos));
    } catch (IOException e) {
      ctx.status(502).result(e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      ctx.status(503).result("La consulta de ubicacion fue interrumpida");
    } catch (IllegalStateException e) {
      ctx.status(503).result("La geocodificacion no esta configurada");
    }
  }

  public void confirmarUbicacion(Context ctx) {
    var encontrada = repositorio.buscarPorId(ctx.pathParam("id"));

    if (encontrada.isEmpty()) {
      ctx.status(404).result("Entidad no encontrada");
      return;
    }

    UbicacionRequest request = ctx.bodyAsClass(UbicacionRequest.class);

    if (request == null
        || request.direccionConsultada() == null
        || request.direccionConsultada().isBlank()
        || request.latitud() == null
        || request.longitud() == null) {
      ctx.status(400).result(
          "Debe indicar direccion consultada, latitud y longitud"
      );
      return;
    }

    var entidad = encontrada.get();

    // Evita confirmar resultados obtenidos para una direccion anterior.
    if (!request.direccionConsultada().equals(entidad.getDireccion())) {
      ctx.status(409).result(
          "La direccion cambio. Volve a consultar sus coordenadas"
      );
      return;
    }

    try {
      entidad.confirmarUbicacion(
          request.latitud(), request.longitud()
      );
    } catch (IllegalArgumentException e) {
      ctx.status(400).result(e.getMessage());
      return;
    }

    repositorio.actualizar(entidad);

    ctx.json(new UbicacionResponse(
        entidad.getIdAsString(),
        entidad.getDireccion(),
        entidad.getLatitud(),
        entidad.getLongitud()
    ));
  }

  public record ResultadoBusqueda(
      String direccionConsultada,
      java.util.List<ClienteGeocodificacion.Candidato> candidatos
  ) {
  }

  public record UbicacionRequest(
      String direccionConsultada,
      Double latitud,
      Double longitud
  ) {
  }

  public record UbicacionResponse(
      String idEntidad,
      String direccion,
      Double latitud,
      Double longitud
  ) {
  }
}