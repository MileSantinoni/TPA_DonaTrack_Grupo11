package org.example.dominio.algoritmo;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Necesidad;
import org.example.dominio.donacion.Donacion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlgoritmoCompatibilidadSemantica {
  private static final int LIMITE_CANDIDATURAS = 10;

  public List<CandidaturaBeneficiaria> proponer(Donacion donacion, List<EntidadBeneficiaria> entidades) {
    List<CandidaturaBeneficiaria> candidaturas = buscarCandidaturas(donacion, entidades);

    Collections.sort(candidaturas, this::compararPorCompatibilidad);

    return candidaturas.subList(0, Math.min(LIMITE_CANDIDATURAS, candidaturas.size()));
  }

  private List<CandidaturaBeneficiaria> buscarCandidaturas(Donacion donacion, List<EntidadBeneficiaria> entidades) {
    List<CandidaturaBeneficiaria> candidaturas = new ArrayList<>();

    for (EntidadBeneficiaria entidad : entidades) {
      for (Necesidad necesidad : entidad.getNecesidades()) {
        if (mismaSubcategoria(donacion, necesidad)) {
          candidaturas.add(new CandidaturaBeneficiaria(donacion, entidad, necesidad));
        }
      }
    }

    return candidaturas;
  }

  private int compararPorCompatibilidad(CandidaturaBeneficiaria c1, CandidaturaBeneficiaria c2) {
    int diferencia1 = calcularDiferencia(c1);
    int diferencia2 = calcularDiferencia(c2);

    if (diferencia1 == 0 && diferencia2 != 0) {
      return -1;
    }

    if (diferencia1 != 0 && diferencia2 == 0) {
      return 1;
    }

    if (diferencia1 < 0 && diferencia2 > 0) {
      return -1;
    }

    if (diferencia1 > 0 && diferencia2 < 0) {
      return 1;
    }

    return Integer.compare(Math.abs(diferencia1), Math.abs(diferencia2));
  }

  private int calcularDiferencia(CandidaturaBeneficiaria candidatura) {
    return candidatura.getNecesidad().getCantidadPendiente() - candidatura.getDonacion().getCantidad();
  }

  private boolean mismaSubcategoria(Donacion donacion, Necesidad necesidad) {
    return donacion.getSubcategoria().getId().equals(necesidad.getSubcategoria().getId());
  }
}
