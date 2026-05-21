package org.example.dominio.catalogo;

import java.time.LocalDate;

public class BienPerecedero extends Bien {
  private LocalDate fechaVencimiento;

  public BienPerecedero(String descripcion, int cantidad, String unidadMedida,
                        Subcategoria subcategoria, LocalDate fechaVencimiento) {
    super(descripcion, cantidad, unidadMedida, subcategoria);
    this.fechaVencimiento = fechaVencimiento;
  }

  public LocalDate getFechaVencimiento() { return fechaVencimiento; }
  public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }
}


