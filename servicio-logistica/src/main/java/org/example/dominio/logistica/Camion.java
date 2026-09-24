package org.example.dominio.logistica;
import javax.persistence.*;

@Entity
@Table(name = "camiones")
public class Camion {

  @Id
  private String patente;
  private double capacidadVolumen; // m³
  private double altura; // m
  private double capacidadCarga; // kg

  @OneToOne(
      mappedBy = "camion",
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  private UbicacionCamion ultimaUbicacion;

  private boolean disponible;

  public Camion(String patente, double capacidadVolumen, double altura, double capacidadCarga) {
    this.patente = patente;
    this.capacidadVolumen = capacidadVolumen;
    this.altura = altura;
    this.capacidadCarga = capacidadCarga;
    this.ultimaUbicacion = null;
    this.disponible = true;
  }

  protected Camion() {
    // Utilizado por JPA.
  }

  public void actualizarUbicacion(UbicacionCamion ubicacion) {
    java.util.Objects.requireNonNull(
        ubicacion, "La ubicacion es obligatoria"
    );

    if (this.ultimaUbicacion == null) {
      this.ultimaUbicacion = new UbicacionCamion(
          ubicacion.getLatitud(),
          ubicacion.getLongitud(),
          ubicacion.getVelocidad(),
          ubicacion.getFechaYHora()
      );
      this.ultimaUbicacion.asociarA(this);
    } else {
      this.ultimaUbicacion.actualizarDesde(ubicacion);
    }
  }

  public void establecerUbicacionInicial(UbicacionCamion ubicacionInicial) {
    actualizarUbicacion(ubicacionInicial);
  }

  public boolean estaDisponible() {
    return disponible;
  }

  public void marcarDisponible() {
    this.disponible = true;
  }

  public void marcarNoDisponible() {
    this.disponible = false;
  }

  public String getPatente() {
    return patente;
  }

  public double getCapacidadVolumen() {
    return capacidadVolumen;
  }

  public double getAltura() {
    return altura;
  }

  public double getCapacidadCarga() {
    return capacidadCarga;
  }

  public UbicacionCamion getUltimaUbicacion() {
    return ultimaUbicacion;
  }
}
