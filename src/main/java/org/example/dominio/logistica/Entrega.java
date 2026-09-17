package org.example.dominio.logistica;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Objects;
import org.example.dominio.notificacion.Notificador;
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
  private LocalDateTime fechaRecepcion;

  public Entrega(Donacion donacion, EntidadBeneficiaria destino, int orden) {
    this.donacion = donacion;
    this.destino = destino;
    this.orden = orden;
    this.estado = EstadoEntrega.PENDIENTE;
    this.fotosRecepcion = new ArrayList<>();
  }

  public void validarInicioTraslado() {
    exigirEstado(EstadoEntrega.PENDIENTE);
    if (donacion.getEstadoActual() != EstadoDonacion.LISTA_PARA_ENTREGAR) {
      throw new IllegalStateException("La donacion debe estar lista para entregar");
    }
  }

  public void iniciarTraslado() {
    validarInicioTraslado();
    donacion.cambiarEstado(EstadoDonacion.EN_TRASLADO, "El chofer inicio la ruta");
    this.estado = EstadoEntrega.EN_TRASLADO;
  }

  public void confirmarRecepcion(Camion camion, Notificador notificador) {
    Objects.requireNonNull(camion, "El camion responsable es obligatorio");
    Objects.requireNonNull(notificador, "El notificador es obligatorio");
    exigirEstado(EstadoEntrega.EN_TRASLADO);
    donacion.cambiarEstado(EstadoDonacion.ENTREGADA, "La entidad confirmo la recepcion");
    this.estado = EstadoEntrega.ENTREGADA;
    this.camionResponsable = camion;
    this.fechaRecepcion = LocalDateTime.now();
    notificador.notificarEntregaExitosa(this);
  }

  public void marcarNoRecibida(String motivo, Notificador notificador) {
    Objects.requireNonNull(notificador, "El notificador es obligatorio");
    validarMotivo(motivo);
    exigirEstado(EstadoEntrega.EN_TRASLADO);
    donacion.cambiarEstado(EstadoDonacion.ENTREGA_FALLIDA, motivo);
    this.estado = EstadoEntrega.NO_RECIBIDA;
    notificador.notificarEntregaNoRecibida(this, motivo);
  }

  public void registrarRetornoADeposito(String motivo) {
    validarMotivo(motivo);
    exigirEstado(EstadoEntrega.NO_RECIBIDA);
    donacion.cambiarEstado(EstadoDonacion.EN_DEPOSITO, motivo);
    this.estado = EstadoEntrega.PENDIENTE;
  }

  public void agregarFotoRecepcion(String fotoUrl) {
    exigirEstado(EstadoEntrega.ENTREGADA);
    if (fotoUrl == null || fotoUrl.isBlank()) {
      throw new IllegalArgumentException("La foto es obligatoria");
    }
    this.fotosRecepcion.add(fotoUrl);
  }

  private void exigirEstado(EstadoEntrega esperado) {
    if (estado != esperado) {
      throw new IllegalStateException("La entrega debe estar " + esperado);
    }
  }

  private void validarMotivo(String motivo) {
    if (motivo == null || motivo.isBlank()) {
      throw new IllegalArgumentException("El motivo es obligatorio");
    }
  }

  public LocalDateTime getFechaRecepcion() { return fechaRecepcion; }
  public Camion getCamionResponsable() { return camionResponsable; }
  public List<String> getFotosRecepcion() { return List.copyOf(fotosRecepcion); }

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
