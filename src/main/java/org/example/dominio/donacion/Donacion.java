package org.example.dominio.donacion;

import org.example.dominio.donante.Donante;


public class Donacion {

  private String descripcionGeneral;
  private int cantidad;
  private String unidadMedida;
  
  public Donacion(String descripcionGeneral, int cantidad, String unidadMedida) {
    this.descripcionGeneral = descripcionGeneral;
    this.cantidad = cantidad;
    this.unidadMedida = unidadMedida;
  }

}
