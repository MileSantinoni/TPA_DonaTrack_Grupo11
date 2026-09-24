package org.example.dominio.logistica;

import java.time.LocalDateTime;
import javax.persistence.*;
import java.util.UUID;


@Entity
@Table(name = "ubicaciones_camion")
public class UbicacionCamion {

  @Id
  private String id = UUID.randomUUID().toString();

  @OneToOne(optional = false)
  @JoinColumn(name = "patente_camion", nullable = false, unique = true)
  private Camion camion;

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

  protected UbicacionCamion() {
    // Utilizado por JPA.
  }

  void asociarA(Camion camion) {
    this.camion = camion;
  }

  void actualizarDesde(UbicacionCamion nueva) {
    this.latitud = nueva.latitud;
    this.longitud = nueva.longitud;
    this.velocidad = nueva.velocidad;
    this.fechaYHora = nueva.fechaYHora;
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
