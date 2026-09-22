package org.example.dominio.logistica;

import java.time.LocalDateTime;
import java.util.List;

/** Contrato JSON interno. Cada microservicio mantiene su propia representacion. */
public record EventoLogistico(String idOperacion, Tipo tipo, List<Referencia> entregas,
                              String patente, String motivo, LocalDateTime fecha) {
  public EventoLogistico {
    if (idOperacion == null || idOperacion.isBlank() || tipo == null
        || entregas == null || entregas.isEmpty() || patente == null
        || patente.isBlank() || motivo == null || motivo.isBlank() || fecha == null) {
      throw new IllegalArgumentException("Evento logistico incompleto");
    }
    entregas = List.copyOf(entregas);
    for (Referencia entrega : entregas) {
      if (entrega.idDonacion() == null || entrega.idDonacion().isBlank()
          || entrega.idEntidad() == null || entrega.idEntidad().isBlank()) {
        throw new IllegalArgumentException("La donacion y la entidad son obligatorias");
      }
    }
    if (tipo != Tipo.INICIO_TRASLADO && entregas.size() != 1) {
      throw new IllegalArgumentException("La operacion corresponde a una sola entrega");
    }
  }
  public enum Tipo { INICIO_TRASLADO, RECEPCION, NO_RECIBIDA, RETORNO_DEPOSITO }
  public record Referencia(String idDonacion, String idEntidad) {}
}
