package org.example.generadorrutas.dto;

import java.util.ArrayList;
import java.util.List;

public class RutaGeneradaResponse {

  private String patenteCamion;
  private UbicacionRequest ubicacionDeposito;
  private List<EntregaGeneradaResponse> entregas = new ArrayList<>();

  public RutaGeneradaResponse() {
  }

  public RutaGeneradaResponse(String patenteCamion, UbicacionRequest ubicacionDeposito) {
    this.patenteCamion = patenteCamion;
    this.ubicacionDeposito = ubicacionDeposito;
  }

  public String getPatenteCamion() {
    return patenteCamion;
  }

  public void setPatenteCamion(String patenteCamion) {
    this.patenteCamion = patenteCamion;
  }

  public UbicacionRequest getUbicacionDeposito() {
    return ubicacionDeposito;
  }

  public void setUbicacionDeposito(UbicacionRequest ubicacionDeposito) {
    this.ubicacionDeposito = ubicacionDeposito;
  }

  public List<EntregaGeneradaResponse> getEntregas() {
    return entregas;
  }

  public void setEntregas(List<EntregaGeneradaResponse> entregas) {
    this.entregas = entregas;
  }
}
