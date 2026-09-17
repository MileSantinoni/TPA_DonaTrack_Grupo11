package org.example.api.donaciones.dto;

import org.example.dominio.beneficiario.Periodicidad;
import java.time.LocalDate;

public class NecesidadRecurrenteRequest {
  private String descripcion;
  private int cantidadObjetivo;
  private String idSubcategoria;
  private LocalDate fechaInicioPeriodo;
  private Periodicidad periodicidad;


  public String getDescripcion() { return descripcion; }
  public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
  public int getCantidadObjetivo() { return cantidadObjetivo; }
  public void setCantidadObjetivo(int cantidadObjetivo) { this.cantidadObjetivo = cantidadObjetivo; }
  public String getIdSubcategoria() { return idSubcategoria; }
  public void setIdSubcategoria(String idSubcategoria) { this.idSubcategoria = idSubcategoria; }
  public LocalDate getFechaInicioPeriodo() { return fechaInicioPeriodo; }
  public void setFechaInicioPeriodo(LocalDate fechaInicioPeriodo) { this.fechaInicioPeriodo = fechaInicioPeriodo; }
  public Periodicidad getPeriodicidad() { return periodicidad; }
  public void setPeriodicidad(Periodicidad periodicidad) { this.periodicidad = periodicidad; }
}
