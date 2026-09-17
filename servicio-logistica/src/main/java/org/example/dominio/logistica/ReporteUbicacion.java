package org.example.dominio.logistica;

import java.time.LocalDateTime;

// Contrato de integración: mensaje que envía la app móvil del conductor
// mientras la ruta está activa. La plataforma lo recibe, valida y procesa.
public class ReporteUbicacion {

  private String patente;
  private double latitud;
  private double longitud;
  private double velocidad; // km/h
  private LocalDateTime fechaYHora;

  public ReporteUbicacion(String patente, double latitud, double longitud, double velocidad, LocalDateTime fechaYHora) {
    this.patente = patente;
    this.latitud = latitud;
    this.longitud = longitud;
    this.velocidad = velocidad;
    this.fechaYHora = fechaYHora;
  }

  public String getPatente() {
    return patente;
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
