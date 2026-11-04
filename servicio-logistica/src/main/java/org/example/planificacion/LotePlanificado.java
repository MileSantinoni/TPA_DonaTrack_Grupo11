package org.example.planificacion;

import javax.persistence.*;

@Entity
@Table(name="lotes_planificacion")
public class LotePlanificado {
  @Id String id;
  @Column(nullable=false,length=250000) String solicitud;
  @Column(length=250000) String recibo;
  @Column(nullable=false) boolean avisado;
  protected LotePlanificado() {}
  LotePlanificado(String id,String solicitud) {this.id=id;this.solicitud=solicitud;}
}
