package org.example.generadorrutas.dto;

public class CamionRutaRequest {

  private String patente;
  private boolean disponible;

  public String getPatente() {
    return patente;
  }

  public void setPatente(String patente) {
    this.patente = patente;
  }

  public boolean isDisponible() {
    return disponible;
  }

  public void setDisponible(boolean disponible) {
    this.disponible = disponible;
  }
}
