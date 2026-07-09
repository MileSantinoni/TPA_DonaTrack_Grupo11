package org.example.generadorrutas.dto;

public class EntregaGeneradaResponse {

  private String idDonacion;
  private String razonSocialEntidad;
  private String direccionEntidad;
  private String telefonoEntidad;
  private int orden;

  public EntregaGeneradaResponse() {
  }

  public EntregaGeneradaResponse(
      String idDonacion,
      String razonSocialEntidad,
      String direccionEntidad,
      String telefonoEntidad,
      int orden
  ) {
    this.idDonacion = idDonacion;
    this.razonSocialEntidad = razonSocialEntidad;
    this.direccionEntidad = direccionEntidad;
    this.telefonoEntidad = telefonoEntidad;
    this.orden = orden;
  }

  public String getIdDonacion() {
    return idDonacion;
  }

  public void setIdDonacion(String idDonacion) {
    this.idDonacion = idDonacion;
  }

  public String getRazonSocialEntidad() {
    return razonSocialEntidad;
  }

  public void setRazonSocialEntidad(String razonSocialEntidad) {
    this.razonSocialEntidad = razonSocialEntidad;
  }

  public String getDireccionEntidad() {
    return direccionEntidad;
  }

  public void setDireccionEntidad(String direccionEntidad) {
    this.direccionEntidad = direccionEntidad;
  }

  public String getTelefonoEntidad() {
    return telefonoEntidad;
  }

  public void setTelefonoEntidad(String telefonoEntidad) {
    this.telefonoEntidad = telefonoEntidad;
  }

  public int getOrden() {
    return orden;
  }

  public void setOrden(int orden) {
    this.orden = orden;
  }
}
