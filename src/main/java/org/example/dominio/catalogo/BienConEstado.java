package org.example.dominio.catalogo;

public class BienConEstado extends Bien {
  private Estado estado;

  public BienConEstado(String descripcion, int cantidad, String unidadMedida,
                       Subcategoria subcategoria, Estado estado) {
    super(descripcion, cantidad, unidadMedida, subcategoria);
    this.estado = estado;
  }

  public Estado getEstado() { return estado; }
  public void setEstado(Estado estado) { this.estado = estado; }
}