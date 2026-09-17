package org.example.api.donaciones.dto;

public class NecesidadExtraordinariaRequest {
  private String descripcion;
  private int cantidadObjetivo;
  private String idSubcategoria;
  private String motivo;


  public String getDescripcion() { return descripcion; }
  public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
  public int getCantidadObjetivo() { return cantidadObjetivo; }
  public void setCantidadObjetivo(int cantidadObjetivo) { this.cantidadObjetivo = cantidadObjetivo; }
  public String getIdSubcategoria() { return idSubcategoria; }
  public void setIdSubcategoria(String idSubcategoria) { this.idSubcategoria = idSubcategoria; }
  public String getMotivo() { return motivo; }
  public void setMotivo(String motivo) { this.motivo = motivo; }
}