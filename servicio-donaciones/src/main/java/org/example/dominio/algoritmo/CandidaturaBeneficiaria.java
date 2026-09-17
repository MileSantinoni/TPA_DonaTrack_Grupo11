package org.example.dominio.algoritmo;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Necesidad;
import org.example.dominio.donacion.Donacion;

public class CandidaturaBeneficiaria {
  private Donacion donacion;
  private EntidadBeneficiaria entidad;
  private Necesidad necesidad;

  public CandidaturaBeneficiaria(Donacion donacion, EntidadBeneficiaria entidad, Necesidad necesidad) {
    this.donacion = donacion;
    this.entidad = entidad;
    this.necesidad = necesidad;
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public EntidadBeneficiaria getEntidad() {
    return entidad;
  }

  public Necesidad getNecesidad() {
    return necesidad;
  }
}
