package org.example.generadorrutas.dto;

import java.util.ArrayList;
import java.util.List;

public class GenerarRutasRequest {

  private List<AsignacionRutaRequest> asignaciones = new ArrayList<>();
  private List<CamionRutaRequest> camiones = new ArrayList<>();
  private UbicacionRequest ubicacionDeposito;

  public List<AsignacionRutaRequest> getAsignaciones() {
    return asignaciones;
  }

  public void setAsignaciones(List<AsignacionRutaRequest> asignaciones) {
    this.asignaciones = asignaciones;
  }

  public List<CamionRutaRequest> getCamiones() {
    return camiones;
  }

  public void setCamiones(List<CamionRutaRequest> camiones) {
    this.camiones = camiones;
  }

  public UbicacionRequest getUbicacionDeposito() {
    return ubicacionDeposito;
  }

  public void setUbicacionDeposito(UbicacionRequest ubicacionDeposito) {
    this.ubicacionDeposito = ubicacionDeposito;
  }
}
