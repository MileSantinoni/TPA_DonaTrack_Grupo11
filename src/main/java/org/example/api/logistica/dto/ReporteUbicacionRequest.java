package org.example.api.logistica.dto;

import java.time.LocalDateTime;

// Cuerpo JSON que envía la app móvil al reportar su ubicación.
public class ReporteUbicacionRequest {

  private String patente;
  private double latitud;
  private double longitud;
  private double velocidad;
  private LocalDateTime fechaYHora;

  public String getPatente() {
    return patente;
  }

  public void setPatente(String patente) {
    this.patente = patente;
  }

  public double getLatitud() {
    return latitud;
  }

  public void setLatitud(double latitud) {
    this.latitud = latitud;
  }

  public double getLongitud() {
    return longitud;
  }

  public void setLongitud(double longitud) {
    this.longitud = longitud;
  }

  public double getVelocidad() {
    return velocidad;
  }

  public void setVelocidad(double velocidad) {
    this.velocidad = velocidad;
  }

  public LocalDateTime getFechaYHora() {
    return fechaYHora;
  }

  public void setFechaYHora(LocalDateTime fechaYHora) {
    this.fechaYHora = fechaYHora;
  }
}