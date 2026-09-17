package org.example.dominio.algoritmo;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.Donacion;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ResultadoEjecucionAlgoritmos {
  private String id;
  private Donacion donacion;
  private List<EntidadBeneficiaria> entidadesPropuestas;
  private LocalDateTime fechaYHoraEjecucion;

  public ResultadoEjecucionAlgoritmos(Donacion donacion, List<EntidadBeneficiaria> entidadesPropuestas) {
    this.id = UUID.randomUUID().toString();
    this.donacion = donacion;
    this.entidadesPropuestas = new ArrayList<>(entidadesPropuestas);
    this.fechaYHoraEjecucion = LocalDateTime.now();
  }

  public String getId() {
    return id;
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public List<EntidadBeneficiaria> getEntidadesPropuestas() {
    return new ArrayList<>(entidadesPropuestas);
  }

  public LocalDateTime getFechaYHoraEjecucion() {
    return fechaYHoraEjecucion;
  }
}
