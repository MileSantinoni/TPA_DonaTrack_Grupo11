package org.example.api.logistica.dto;

// Respuesta JSON con el avance del camión sobre su ruta (0 a 100).
public class AvanceResponse {

  private String patente;
  private double porcentajeAvance;

  public AvanceResponse(String patente, double porcentajeAvance) {
    this.patente = patente;
    this.porcentajeAvance = porcentajeAvance;
  }

  public String getPatente() {
    return patente;
  }

  public double getPorcentajeAvance() {
    return porcentajeAvance;
  }
}