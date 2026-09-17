package org.example.api.logistica.dto;

import java.util.List;

// Cuerpo JSON para registrar una ruta (patente + entregas ordenadas).
public class RutaRequest {

  private String patente;
  private Double latitudDeposito;
  private Double longitudDeposito;
  private List<EntregaRequest> entregas;

  public String getPatente() {
    return patente;
  }

  public void setPatente(String patente) {
    this.patente = patente;
  }

  public Double getLatitudDeposito() {
    return latitudDeposito;
  }

  public void setLatitudDeposito(Double latitudDeposito) {
    this.latitudDeposito = latitudDeposito;
  }

  public Double getLongitudDeposito() {
    return longitudDeposito;
  }

  public void setLongitudDeposito(Double longitudDeposito) {
    this.longitudDeposito = longitudDeposito;
  }

  public List<EntregaRequest> getEntregas() {
    return entregas;
  }

  public void setEntregas(List<EntregaRequest> entregas) {
    this.entregas = entregas;
  }

  public static class EntregaRequest {

    private String idDonacion;
    private String razonSocial;
    private String direccion;
    private String telefono;
    private int orden;

    public String getIdDonacion() {
      return idDonacion;
    }

    public void setIdDonacion(String idDonacion) {
      this.idDonacion = idDonacion;
    }

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
  public org.example.dominio.logistica.PlanDeRuta aPlan() {
    var deposito = latitudDeposito == null || longitudDeposito == null ? null
        : new org.example.dominio.logistica.UbicacionCamion(
            latitudDeposito, longitudDeposito, 0, java.time.LocalDateTime.now());
    return new org.example.dominio.logistica.PlanDeRuta(patente,
        entregas.stream().map(e -> new org.example.dominio.logistica.PlanDeRuta.Destino(
            e.getIdDonacion(), e.getRazonSocial(), e.getDireccion(), e.getTelefono(), e.getOrden()))
            .toList(), deposito);
  }

}
