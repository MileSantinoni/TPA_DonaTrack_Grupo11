package org.example.controllers;

import io.javalin.http.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.Donacion;

public class LandingController {

  private final RepositorioEntidadesBeneficiarias repositorioEntidades;
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioAsignacionesDonacion repositorioAsignaciones;

  public LandingController(
      RepositorioEntidadesBeneficiarias repositorioEntidades,
      RepositorioDonaciones repositorioDonaciones,
      RepositorioAsignacionesDonacion repositorioAsignaciones) {
    this.repositorioEntidades = repositorioEntidades;
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioAsignaciones = repositorioAsignaciones;
  }

  public void index(Context ctx) {
    renderizar(ctx, new HashMap<>());
  }

  public void renderizar(Context ctx, Map<String, Object> model) {

    // Datos para el mapa
    List<EntidadBeneficiaria> entidades = repositorioEntidades.buscarTodas();
    List<AsignacionDonacion> asignaciones = repositorioAsignaciones.buscarTodas();
    List<Donacion> donaciones = repositorioDonaciones.buscarTodas();

    model.put("titulo", "Bienvenido");
    model.put(
        "entidades",
        entidades.stream()
            .filter(e -> e.getLatitud() != null && e.getLongitud() != null)
            .toList()
    );
    model.put("asignaciones", asignaciones);
    model.put("donaciones", donaciones);

    ctx.render("landingPage.hbs", model);
  }
}