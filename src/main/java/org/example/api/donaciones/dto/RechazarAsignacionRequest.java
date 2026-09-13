package org.example.api.donaciones.dto;

public class RechazarAsignacionRequest {

  private String idDonacion;
  private String motivo;

  public RechazarAsignacionRequest() {
  }

  public RechazarAsignacionRequest(String idDonacion, String motivo) {
    this.idDonacion = idDonacion;
    this.motivo = motivo;
  }

  public String getIdDonacion() {
    return idDonacion;
  }

  public void setIdDonacion(String idDonacion) {
    this.idDonacion = idDonacion;
  }

  public String getMotivo() {
    return motivo;
  }

  public void setMotivo(String motivo) {
    this.motivo = motivo;
  }
}