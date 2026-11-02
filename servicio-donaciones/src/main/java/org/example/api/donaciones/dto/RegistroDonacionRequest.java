package org.example.api.donaciones.dto;

import java.time.LocalDate;
import java.util.List;
import org.example.dominio.catalogo.Estado;

public record RegistroDonacionRequest(
    String idDonante,
    String descripcionGeneral,
    List<BienRequest> bienes
) {
  public record BienRequest(
      String descripcion,
      int cantidad,
      String unidadMedida,
      String idSubcategoria,
      LocalDate fechaVencimiento,
      Estado estadoBien,
      String foto
  ) {
  }
}