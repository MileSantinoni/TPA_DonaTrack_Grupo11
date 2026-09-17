package org.example.dominio.beneficiario;

import org.example.dominio.catalogo.Subcategoria;

public class NecesidadExtraordinaria extends Necesidad {
  private String motivo;

  public NecesidadExtraordinaria(String descripcion, int cantidadObjetivo, Subcategoria subcategoria, String motivo) {
    super(descripcion, cantidadObjetivo, subcategoria);
    this.motivo = motivo;
  }

  @Override
  public boolean estaSatisfecha() {
    // Se satisface cuando la cantidad recibida iguala o supera lo requerido
    return this.cantidadCubierta >= this.cantidadObjetivo;
  }
}
