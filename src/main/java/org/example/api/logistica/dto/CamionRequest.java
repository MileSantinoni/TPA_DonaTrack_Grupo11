package org.example.api.logistica.dto;

// Cuerpo JSON para dar de alta un camión en la flota.
public class CamionRequest {

  private String patente;
  private double capacidadVolumen;
  private double altura;
  private double capacidadCarga;

  public String getPatente() {
    return patente;
  }

  public void setPatente(String patente) {
    this.patente = patente;
  }

  public double getCapacidadVolumen() {
    return capacidadVolumen;
  }

  public void setCapacidadVolumen(double capacidadVolumen) {
    this.capacidadVolumen = capacidadVolumen;
  }

  public double getAltura() {
    return altura;
  }

  public void setAltura(double altura) {
    this.altura = altura;
  }

  public double getCapacidadCarga() {
    return capacidadCarga;
  }

  public void setCapacidadCarga(double capacidadCarga) {
    this.capacidadCarga = capacidadCarga;
  }
}