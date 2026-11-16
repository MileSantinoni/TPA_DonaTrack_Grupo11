package org.example.dominio.beneficiario;

import java.time.LocalDate;
import javax.persistence.*;

@Embeddable
public class AporteNecesidad {

  @Column(nullable = false)
  private int cantidad;

  @Column(nullable = false)
  private LocalDate fecha;

  protected AporteNecesidad() {
  }

  public AporteNecesidad(int cantidad, LocalDate fecha) {
    if (cantidad <= 0 || fecha == null) {
      throw new IllegalArgumentException(
          "El aporte requiere cantidad positiva y fecha."
      );
    }

    this.cantidad = cantidad;
    this.fecha = fecha;
  }

  public int getCantidad() {
    return cantidad;
  }

  public LocalDate getFecha() {
    return fecha;
  }
}