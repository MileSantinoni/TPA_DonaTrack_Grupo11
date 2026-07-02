package org.example.dominio.logistica;

public class Camion {

  private String patente;
  private double capacidadVolumen; // m³
  private double altura; // m
  private double capacidadCarga; // kg
  private UbicacionCamion ultimaUbicacion;

  public Camion(String patente, double capacidadVolumen, double altura, double capacidadCarga) {
    this.patente = patente;
    this.capacidadVolumen = capacidadVolumen;
    this.altura = altura;
    this.capacidadCarga = capacidadCarga;
    this.ultimaUbicacion = null;
  }

  public void actualizarUbicacion(UbicacionCamion ubicacion) {
    this.ultimaUbicacion = ubicacion;
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