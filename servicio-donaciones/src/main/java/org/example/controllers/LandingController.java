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
import org.example.dominio.donacion.EstadoDonacion;

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

  public void cargarLanding(Context ctx) {
    List<EntidadBeneficiaria> entidades = repositorioEntidades.buscarTodas();
    List<AsignacionDonacion> asignaciones = repositorioAsignaciones.buscarTodas();
    List<Donacion> donaciones = repositorioDonaciones.buscarTodas();

    ctx.json(Map.of(
        "entidades", entidades.stream()
            .filter(e -> e.getLatitud() != null && e.getLongitud() != null)
            .map(e -> Map.of(
                "id", e.getIdAsString(),
                "razonSocial", e.getRazonSocial(),
                "direccion", e.getDireccion(),
                "telefono", e.getTelefono(),
                "latitud", e.getLatitud(),
                "longitud", e.getLongitud()
            ))
            .toList(),
        "asignaciones", asignaciones.stream()
            .map(a -> Map.of(
                "id", a.getIdAsString(),
                "entidadId", a.getEntidad().getIdAsString(),
                "donacionId", a.getDonacion().getIdAsString()
            ))
            .toList(),
        "donaciones", donaciones.stream()
            .map(d -> Map.of(
                "id", d.getIdAsString(),
                "descripcionGeneral", d.getDescripcionGeneral(),
                "cantidad", d.getCantidad(),
                "unidadMedida", d.getUnidadMedida()
//                "estadoActualTexto", d.getEstadoActualTexto()
            ))
            .toList()
    ));
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

    // Estadísticas dinámicas para la sección "Impacto en números"
    long donacionesTotales = donaciones.size();
    long entidadesConectadas = entidades.stream()
        .filter(e -> e.getLatitud() != null && e.getLongitud() != null)
        .count();
    long entregasActivas = asignaciones.stream()
        .filter(a -> a.getDonacion() != null)
        .filter(a -> {
            EstadoDonacion estado = a.getDonacion().getEstadoActual();
            // .getNombre() se lo saqué del final
            return estado != EstadoDonacion.ENTREGADA
                && estado != EstadoDonacion.ENTREGA_FALLIDA
                && estado != EstadoDonacion.VENCIDA;
        })
        .count();

    model.put("stats", Map.of(
        "donacionesTotales", donacionesTotales,
        "entidadesConectadas", entidadesConectadas,
        "entregasActivas", entregasActivas
    ));

    ctx.render("landingPage.hbs", model);
  }

  public void fotosPublicas(Context ctx) {
    Map<String, Object> model = new HashMap<>();
    model.put("titulo", "Fotos de Donaciones Entregadas");
    ctx.render("fotos-publicas.hbs", model);
  }
}