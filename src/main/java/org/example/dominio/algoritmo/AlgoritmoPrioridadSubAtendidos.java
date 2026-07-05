package org.example.dominio.algoritmo;

import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.AsignacionDonacion;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlgoritmoPrioridadSubAtendidos {
  private static final int LIMITE_ENTIDADES = 10;
  private final RepositorioAsignacionesDonacion repositorioAsignaciones;

  public AlgoritmoPrioridadSubAtendidos() {
    this(RepositorioAsignacionesDonacion.getInstance());
  }

  public AlgoritmoPrioridadSubAtendidos(RepositorioAsignacionesDonacion repositorioAsignaciones) {
    this.repositorioAsignaciones = repositorioAsignaciones;
  }

  public List<EntidadBeneficiaria> proponer(List<EntidadBeneficiaria> entidades) {
    LocalDate fechaReferencia = LocalDate.now();
    LocalDate inicioTrimestre = fechaReferencia.minusMonths(3);
    List<AsignacionDonacion> asignacionesUltimoTrimestre =
        repositorioAsignaciones.buscarEntreFechas(inicioTrimestre, fechaReferencia);

    List<EntidadBeneficiaria> entidadesOrdenadas = new ArrayList<>(entidades);

    Collections.sort(entidadesOrdenadas, (e1, e2) ->
        compararPorMenorAtencion(e1, e2, asignacionesUltimoTrimestre)
    );

    return entidadesOrdenadas.subList(0, Math.min(LIMITE_ENTIDADES, entidadesOrdenadas.size()));
  }

  private int compararPorMenorAtencion(EntidadBeneficiaria e1,
                                       EntidadBeneficiaria e2,
                                       List<AsignacionDonacion> asignaciones) {
    int recibidas1 = contarRecepciones(e1, asignaciones);
    int recibidas2 = contarRecepciones(e2, asignaciones);

    if (recibidas1 != recibidas2) {
      return Integer.compare(recibidas1, recibidas2);
    }

    return e1.getRazonSocial().compareToIgnoreCase(e2.getRazonSocial()); //Si tienen la misma cantidad desempatan por orden alfabetico
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
