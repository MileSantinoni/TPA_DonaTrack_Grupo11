package org.example.dominio.beneficiario;

import org.example.dominio.catalogo.Subcategoria;
import javax.persistence.*;


@Entity
@Table(name = "necesidades_extraordinarias")
public class NecesidadExtraordinaria extends Necesidad {
  private String motivo;

  protected NecesidadExtraordinaria() {}
  public NecesidadExtraordinaria(String descripcion, int cantidadObjetivo, Subcategoria subcategoria, String motivo) {
    super(descripcion, cantidadObjetivo, subcategoria);
    this.motivo = motivo;
  }

  @Override
  public boolean estaSatisfecha() {
    // Se satisface cuando la cantidad recibida iguala o supera lo requerido
    return this.cantidadCubierta >= this.cantidadObjetivo;
  }

  public String getMotivo() {
    return motivo;
  }

  public void actualizarMotivo(String motivo) {
    if (motivo == null || motivo.isBlank()) {
      throw new IllegalArgumentException(
          "El motivo es obligatorio."
      );
    }

    this.motivo = motivo.trim();
  }

  public String getDescripcion() { return descripcion; }
}
