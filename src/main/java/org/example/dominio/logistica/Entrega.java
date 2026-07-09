package org.example.dominio.logistica;

import java.util.List;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;

public class Entrega {

  private Donacion donacion;
  private EntidadBeneficiaria destino;
  private Camion camionResponsable;
  private int orden;
  private EstadoEntrega estado;
  private List<String> fotosRecepcion;

  public Entrega(Donacion donacion, EntidadBeneficiaria destino, int orden) {
    this.donacion = donacion;
    this.destino = destino;
    this.orden = orden;
    this.estado = EstadoEntrega.PENDIENTE;
  }

  public void marcarEnTraslado() {
    this.estado = EstadoEntrega.EN_TRASLADO;
  }

  public void marcarEntregada(Camion camion) {
    this.estado = EstadoEntrega.ENTREGADA;
    this.camionResponsable = camion;
  }

  public void marcarNoRecibida() {
    this.estado = EstadoEntrega.NO_RECIBIDA;
  }

  public void volverAPendiente() {
    this.estado = EstadoEntrega.PENDIENTE;
  }

  public void agregarFotoRecepcion(String fotoUrl) {
    this.fotosRecepcion.add(fotoUrl);
  }

  public boolean fueResuelta() {
    if (estado == EstadoEntrega.ENTREGADA) {
      return true;
    }
    if (estado == EstadoEntrega.NO_RECIBIDA) {
      return true;
    }
    return false;
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public EntidadBeneficiaria getDestino() {
    return destino;
  }

  public int getOrden() {
    return orden;
  }

  public EstadoEntrega getEstado() {
    return estado;
  }
}