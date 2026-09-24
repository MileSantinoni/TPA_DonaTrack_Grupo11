package org.example.dominio.logistica;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ubicaciones_deposito")
public class UbicacionDeposito {

  @Id
  private String id = UUID.randomUUID().toString();

  private double latitud;
  private double longitud;

  protected UbicacionDeposito() {
    // Utilizado por JPA.
  }

  public UbicacionDeposito(double latitud, double longitud) {
    this.latitud = latitud;
    this.longitud = longitud;
  }

  public UbicacionCamion comoUbicacionInicial() {
    return new UbicacionCamion(
        latitud,
        longitud,
        0,
        LocalDateTime.now()
    );
  }

  public String getId() {
    return id;
  }

  public double getLatitud() {
    return latitud;
  }

  public double getLongitud() {
    return longitud;
  }
}