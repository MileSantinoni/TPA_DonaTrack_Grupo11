package org.example.dominio.donacion;

import java.time.LocalDateTime;

public class RegistroCambioEstado {

  private EstadoDonacion estadoAnterior;
  private EstadoDonacion estadoNuevo;
  private LocalDateTime fechaYHora;
  private String justificativo;

  public RegistroCambioEstado(EstadoDonacion estadoAnterior, EstadoDonacion estadoNuevo, String justificativo) {
    this.estadoAnterior = estadoAnterior;
    this.estadoNuevo = estadoNuevo;
    this.justificativo = justificativo;
    this.fechaYHora = LocalDateTime.now();
  }

  public EstadoDonacion getEstadoAnterior() { return estadoAnterior;}

  public EstadoDonacion getEstadoNuevo() { return estadoNuevo; }

  public LocalDateTime getFechaYHora() { return fechaYHora;}

  public String getJustificativo() { return justificativo;}
  
  
}

