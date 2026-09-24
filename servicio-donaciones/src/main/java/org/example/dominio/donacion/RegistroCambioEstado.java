package org.example.dominio.donacion;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.*;

@Entity
@Table(name = "registro_cambio_estado")
public class RegistroCambioEstado {

  @Id
  @GeneratedValue
  private UUID id;

  @Column(name = "estado_anterior")
  private EstadoDonacion estadoAnterior;

//  @Enumerated(EnumType.STRING)
  @Column(name = "estado_nuevo")
  private EstadoDonacion estadoNuevo;

  @Column(name = "fecha_y_hora")
  private LocalDateTime fechaYHora;
  private String justificativo;

  protected RegistroCambioEstado() {}

  public RegistroCambioEstado(EstadoDonacion estadoAnterior, EstadoDonacion estadoNuevo, String justificativo) {
    this.estadoAnterior = estadoAnterior; //podría ser una lista de estados anteriores
    this.estadoNuevo = estadoNuevo;
    this.justificativo = justificativo;
    this.fechaYHora = LocalDateTime.now();
  }

  public EstadoDonacion getEstadoAnterior() { return estadoAnterior;}

  public EstadoDonacion getEstadoNuevo() { return estadoNuevo; }

  public LocalDateTime getFechaYHora() { return fechaYHora;}

  public String getJustificativo() { return justificativo;}


}

