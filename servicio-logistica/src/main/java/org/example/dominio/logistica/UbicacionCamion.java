package org.example.dominio.logistica;

import java.time.LocalDateTime;

public class UbicacionCamion {

  private double latitud;
  private double longitud;
  private double velocidad; // km/h
  private LocalDateTime fechaYHora;

  public UbicacionCamion(double latitud, double longitud, double velocidad, LocalDateTime fechaYHora) {
    this.latitud = latitud;
    this.longitud = longitud;
    this.velocidad = velocidad;
    this.fechaYHora = fechaYHora;
  }

  public double getLatitud() {
    return latitud;
  }

  public double getLongitud() {
    return longitud;
  }

  public double getVelocidad() {
    return velocidad;
  }

  public LocalDateTime getFechaYHora() {
    return fechaYHora;
  }
}
