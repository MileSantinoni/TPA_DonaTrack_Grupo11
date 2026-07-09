package org.example.generadorrutas.dto;

public class AsignacionRutaRequest {

  private String idDonacion;
  private EstadoDonacionDto estadoDonacion;
  private String razonSocialEntidad;
  private String direccionEntidad;
  private String telefonoEntidad;

  public String getIdDonacion() {
    return idDonacion;
  }

  public void setIdDonacion(String idDonacion) {
    this.idDonacion = idDonacion;
  }

  public EstadoDonacionDto getEstadoDonacion() {
    return estadoDonacion;
  }

  public void setEstadoDonacion(EstadoDonacionDto estadoDonacion) {
    this.estadoDonacion = estadoDonacion;
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
}
