package org.example.api.donaciones.dto;

import org.example.dominio.donacion.EstadoDonacion;

public class CambioEstadoRequest {
  private EstadoDonacion nuevoEstado;
  private String justificativo;


  public EstadoDonacion getNuevoEstado() { return nuevoEstado; }
  public void setNuevoEstado(EstadoDonacion nuevoEstado) { this.nuevoEstado = nuevoEstado; }
  public String getJustificativo() { return justificativo; }
  public void setJustificativo(String justificativo) { this.justificativo = justificativo; }
}
