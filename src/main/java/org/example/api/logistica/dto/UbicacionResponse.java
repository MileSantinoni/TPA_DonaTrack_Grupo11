package org.example.api.logistica.dto;

import java.time.LocalDateTime;

// Respuesta JSON que consume el dashboard para ubicar al camión.
public class UbicacionResponse {

  private double latitud;
  private double longitud;
  private double velocidad;
  private LocalDateTime fechaYHora;

  public UbicacionResponse(double latitud, double longitud, double velocidad, LocalDateTime fechaYHora) {
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
