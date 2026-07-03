package org.example.api.donaciones.dto;

import org.example.dominio.catalogo.Estado;
import java.time.LocalDate;

public class DonacionRequest {
  private String descripcionGeneral;
  private int cantidad;
  private String unidadMedida;
  private String idSubcategoria;
  private LocalDate fechaVencimiento;
  private Estado estadoBien;


  public String getDescripcionGeneral() { return descripcionGeneral; }
  public void setDescripcionGeneral(String descripcionGeneral) { this.descripcionGeneral = descripcionGeneral; }

  public int getCantidad() { return cantidad; }
  public void setCantidad(int cantidad) { this.cantidad = cantidad; }

  public String getUnidadMedida() { return unidadMedida; }
  public void setUnidadMedida(String unidadMedida) { this.unidadMedida = unidadMedida; }

  public String getIdSubcategoria() { return idSubcategoria; }
  public void setIdSubcategoria(String idSubcategoria) { this.idSubcategoria = idSubcategoria; }

  public LocalDate getFechaVencimiento() { return fechaVencimiento; }
  public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

  public Estado getEstadoBien() { return estadoBien; }
  public void setEstadoBien(Estado estadoBien) { this.estadoBien = estadoBien; }
}