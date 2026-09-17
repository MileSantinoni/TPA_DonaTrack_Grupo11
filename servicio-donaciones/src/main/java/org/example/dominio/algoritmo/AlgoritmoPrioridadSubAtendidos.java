package org.example.dominio.algoritmo;

import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.AsignacionDonacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlgoritmoPrioridadSubAtendidos {
  private static final int LIMITE_CANDIDATURAS = 10;
  private final RepositorioAsignacionesDonacion repositorioAsignaciones;

  public AlgoritmoPrioridadSubAtendidos() {
    this(RepositorioAsignacionesDonacion.getInstance());
  }

  public AlgoritmoPrioridadSubAtendidos(RepositorioAsignacionesDonacion repositorioAsignaciones) {
    this.repositorioAsignaciones = repositorioAsignaciones;
  }

  public List<CandidaturaBeneficiaria> proponer(List<CandidaturaBeneficiaria> candidaturas) {
    LocalDate fechaReferencia = LocalDate.now();
    LocalDate inicioTrimestre = fechaReferencia.minusMonths(3);
    List<AsignacionDonacion> asignacionesUltimoTrimestre =
        repositorioAsignaciones.buscarEntreFechas(inicioTrimestre, fechaReferencia);

    List<CandidaturaBeneficiaria> candidaturasOrdenadas = new ArrayList<>(candidaturas);

    Collections.sort(candidaturasOrdenadas, (c1, c2) ->
        compararPorMenorAtencion(c1, c2, asignacionesUltimoTrimestre)
    );

    return candidaturasOrdenadas.subList(0, Math.min(LIMITE_CANDIDATURAS, candidaturasOrdenadas.size()));
  }

  private int compararPorMenorAtencion(CandidaturaBeneficiaria c1,
                                       CandidaturaBeneficiaria c2,
                                       List<AsignacionDonacion> asignaciones) {
    int recibidas1 = contarRecepciones(c1.getEntidad(), asignaciones);
    int recibidas2 = contarRecepciones(c2.getEntidad(), asignaciones);

    if (recibidas1 != recibidas2) {
      return Integer.compare(recibidas1, recibidas2);
    }

    return c1.getEntidad().getRazonSocial().compareToIgnoreCase(c2.getEntidad().getRazonSocial());
  }

  private int contarRecepciones(EntidadBeneficiaria entidad,
                                List<AsignacionDonacion> asignaciones) {
    int cantidad = 0;

    for (AsignacionDonacion asignacion : asignaciones) {
      boolean mismaEntidad = asignacion.getEntidad().getId().equals(entidad.getId());

      if (mismaEntidad) {
        cantidad++;
      }
    }

    return cantidad;
  }
}
