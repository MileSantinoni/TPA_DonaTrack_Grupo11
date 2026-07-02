package org.example.api.logistica.dto;

import java.util.List;

// Cuerpo JSON para registrar una ruta (patente + entregas ordenadas).
public class RutaRequest {

  private String patente;
  private List<EntregaRequest> entregas;

  public String getPatente() {
    return patente;
  }

  public void setPatente(String patente) {
    this.patente = patente;
  }

  public List<EntregaRequest> getEntregas() {
    return entregas;
  }

  public void setEntregas(List<EntregaRequest> entregas) {
    this.entregas = entregas;
  }

  public static class EntregaRequest {

    private String razonSocial;
    private String direccion;
    private String telefono;
    private int orden;

    public String getRazonSocial() {
      return razonSocial;
    }

    public void setRazonSocial(String razonSocial) {
      this.razonSocial = razonSocial;
    }

    public String getDireccion() {
      return direccion;
    }

    public void setDireccion(String direccion) {
      this.direccion = direccion;
    }

    public String getTelefono() {
      return telefono;
    }

    public void setTelefono(String telefono) {
      this.telefono = telefono;
    }

    public int getOrden() {
      return orden;
    }

    public void setOrden(int orden) {
      this.orden = orden;
    }
  }
}
