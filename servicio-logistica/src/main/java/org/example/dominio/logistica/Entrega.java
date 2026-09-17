package org.example.dominio.logistica;

import java.util.ArrayList;
import java.util.List;

// Logistica conserva una referencia y los datos de destino recibidos por HTTP.
// Donacion y EntidadBeneficiaria pertenecen al otro microservicio.
public class Entrega {
  private final String idDonacion;
  private final String idEntidad;
  private final String razonSocial;
  private final String direccion;
  private final String telefono;
  private final int orden;
  private EstadoEntrega estado = EstadoEntrega.PENDIENTE;
  private Camion camionResponsable;
  private final List<String> fotosRecepcion = new ArrayList<>();

  public Entrega(String idDonacion, String idEntidad, String razonSocial,
                 String direccion, String telefono, int orden) {
    this.idDonacion = idDonacion;
    this.idEntidad = idEntidad;
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
    this.orden = orden;
  }

  public void marcarEnTraslado() { estado = EstadoEntrega.EN_TRASLADO; }
  public void marcarEntregada(Camion camion) {
    estado = EstadoEntrega.ENTREGADA;
    camionResponsable = camion;
  }
  public void marcarNoRecibida() { estado = EstadoEntrega.NO_RECIBIDA; }
  public void volverAPendiente() { estado = EstadoEntrega.PENDIENTE; }
  public void agregarFotoRecepcion(String foto) { fotosRecepcion.add(foto); }
  public boolean fueResuelta() {
    return estado == EstadoEntrega.ENTREGADA || estado == EstadoEntrega.NO_RECIBIDA;
  }

  public String getIdDonacion() { return idDonacion; }
  public String getIdEntidad() { return idEntidad; }
  public String getRazonSocial() { return razonSocial; }
  public String getDireccion() { return direccion; }
  public String getTelefono() { return telefono; }
  public int getOrden() { return orden; }
  public EstadoEntrega getEstado() { return estado; }
  public Camion getCamionResponsable() { return camionResponsable; }
  public List<String> getFotosRecepcion() { return List.copyOf(fotosRecepcion); }
}
