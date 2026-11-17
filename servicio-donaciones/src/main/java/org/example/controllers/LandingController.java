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

    // Mapa de donacionId -> entidad para consultar coordenadas rapidamente
    Map<String, EntidadBeneficiaria> donacionEntidadMap = new HashMap<>();
    for (AsignacionDonacion asignacion : asignaciones) {
      if (asignacion.getDonacion() != null && asignacion.getEntidad() != null) {
        String donacionId = asignacion.getDonacion().getIdAsString();
        if (donacionId != null && !donacionEntidadMap.containsKey(donacionId)) {
          donacionEntidadMap.put(donacionId, asignacion.getEntidad());
        }
      }
    }

    List<Map<String, Object>> entidadesDto = entidades.stream()
        .filter(e -> e.getLatitud() != null && e.getLongitud() != null)
        .map(e -> {
          Map<String, Object> map = new HashMap<>();
          map.put("id", e.getIdAsString());
          map.put("razonSocial", e.getRazonSocial() != null ? e.getRazonSocial() : "");
          map.put("direccion", e.getDireccion() != null ? e.getDireccion() : "");
          map.put("telefono", e.getTelefono() != null ? e.getTelefono() : "");
          map.put("latitud", e.getLatitud());
          map.put("longitud", e.getLongitud());
          return map;
        })
        .toList();

    List<Map<String, Object>> asignacionesDto = asignaciones.stream()
        .filter(a -> a.getEntidad() != null && a.getDonacion() != null)
        .map(a -> {
          Map<String, Object> map = new HashMap<>();
          map.put("id", a.getIdAsString());
          map.put("entidadId", a.getEntidad().getIdAsString());
          map.put("donacionId", a.getDonacion().getIdAsString());
          map.put("estado", a.getDonacion().getEstadoActualTexto() != null
              ? a.getDonacion().getEstadoActualTexto()
              : (a.getDonacion().getEstadoActual() != null ? a.getDonacion().getEstadoActual().name() : "EN_DEPOSITO"));
          map.put("fechaRecepcion", a.getFechaRecepcion() != null ? a.getFechaRecepcion().toString() : null);
          return map;
        })
        .toList();

    List<Map<String, Object>> donacionesDto = donaciones.stream()
        .map(d -> {
          String donacionId = d.getIdAsString();
          EntidadBeneficiaria entidad = donacionEntidadMap.get(donacionId);
          Double latitud = entidad != null ? entidad.getLatitud() : null;
          Double longitud = entidad != null ? entidad.getLongitud() : null;
          Map<String, Object> donacionMap = new HashMap<>();
          donacionMap.put("id", d.getIdAsString());
          donacionMap.put("descripcionGeneral", d.getDescripcionGeneral() != null ? d.getDescripcionGeneral() : "");
          donacionMap.put("cantidad", d.getCantidad());
          donacionMap.put("unidadMedida", d.getUnidadMedida() != null ? d.getUnidadMedida() : "");
          donacionMap.put("estadoActualTexto", d.getEstadoActualTexto() != null ? d.getEstadoActualTexto() : (d.getEstadoActual() != null ? d.getEstadoActual().name() : "EN_DEPOSITO"));
          donacionMap.put("latitud", latitud);
          donacionMap.put("longitud", longitud);
          return donacionMap;
        })
        .toList();

    Map<String, Object> respuesta = new HashMap<>();
    respuesta.put("entidades", entidadesDto);
    respuesta.put("asignaciones", asignacionesDto);
    respuesta.put("donaciones", donacionesDto);

    ctx.json(respuesta);
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

    // Estadisticas dinamicas para la seccion "Impacto en numeros"
    long donacionesTotales = donaciones.size();
    long entidadesConectadas = entidades.stream()
        .filter(e -> e.getLatitud() != null && e.getLongitud() != null)
        .count();
    long entregasActivas = asignaciones.stream()
        .filter(a -> a.getDonacion() != null)
        .filter(a -> {
          EstadoDonacion estado = a.getDonacion().getEstadoActual();
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
}
