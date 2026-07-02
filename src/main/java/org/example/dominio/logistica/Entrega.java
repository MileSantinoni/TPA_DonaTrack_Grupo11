package org.example.dominio.logistica;

import org.example.dominio.beneficiario.EntidadBeneficiaria;

public class Entrega {

  private EntidadBeneficiaria destino;
  private int orden; // posición dentro de la ruta
  private EstadoEntrega estado;

  public Entrega(EntidadBeneficiaria destino, int orden) {
    this.destino = destino;
    this.orden = orden;
    this.estado = EstadoEntrega.PENDIENTE;
  }

  public void marcarEnTraslado() {
    this.estado = EstadoEntrega.EN_TRASLADO;
  }

  public void marcarEntregada() {
    this.estado = EstadoEntrega.ENTREGADA;
  }

  public void marcarNoRecibida() {
    this.estado = EstadoEntrega.NO_RECIBIDA;
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