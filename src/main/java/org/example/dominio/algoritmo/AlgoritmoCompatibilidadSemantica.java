package org.example.dominio.algoritmo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlgoritmoCompatibilidadSemantica {
  private static final int LIMITE_CANDIDATURAS = 10;

  public List<CandidaturaBeneficiaria> proponer(List<CandidaturaBeneficiaria> candidaturas) {
    List<CandidaturaBeneficiaria> candidaturasOrdenadas = new ArrayList<>(candidaturas);

    Collections.sort(candidaturasOrdenadas, this::compararPorCompatibilidad);

    return candidaturasOrdenadas.subList(0, Math.min(LIMITE_CANDIDATURAS, candidaturasOrdenadas.size()));
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
}
