package org.example.Repositorios;

import org.example.dominio.donacion.AsignacionDonacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepositorioAsignacionesDonacion {
  private static RepositorioAsignacionesDonacion instancia;
  private final List<AsignacionDonacion> asignaciones;

  private RepositorioAsignacionesDonacion() {
    this.asignaciones = new ArrayList<>();
  }

  public static RepositorioAsignacionesDonacion getInstance() {
    if (instancia == null) {
      instancia = new RepositorioAsignacionesDonacion();
    }
    return instancia;
  }

  public void agregar(AsignacionDonacion asignacion) {
    this.asignaciones.add(asignacion);
  }

  public void eliminar(AsignacionDonacion asignacion) {
    this.asignaciones.remove(asignacion);
  }

  public List<AsignacionDonacion> buscarTodas() {
    return new ArrayList<>(asignaciones);
  }

  public Optional<AsignacionDonacion> buscarPorId(String id) {
    return asignaciones.stream()
        .filter(asignacion -> asignacion.getId().equals(id))
        .findFirst();
  }

  public List<AsignacionDonacion> buscarPorEntidad(String idEntidad) {
    return asignaciones.stream()
        .filter(asignacion -> asignacion.getEntidad().getId().equals(idEntidad))
        .toList();
  }

  public List<AsignacionDonacion> buscarEntreFechas(LocalDate desde, LocalDate hasta) {
    return asignaciones.stream()
        .filter(asignacion -> asignacion.getFechaRecepcion() != null)
        .filter(asignacion -> !asignacion.getFechaRecepcion().isBefore(desde))
        .filter(asignacion -> !asignacion.getFechaRecepcion().isAfter(hasta))
        .toList();
  }

  public void limpiar() {
    this.asignaciones.clear();
  }
}
