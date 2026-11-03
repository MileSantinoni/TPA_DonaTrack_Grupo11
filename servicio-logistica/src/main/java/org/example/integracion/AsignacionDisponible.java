package org.example.integracion;

import java.util.List;

public record AsignacionDisponible(
    String idDonacion,
    String idEntidad,
    String razonSocial,
    String direccion,
    String telefono,
    String estadoDonacion,
    Double latitud,
    Double longitud
) {
  // Conserva los llamados existentes que no indican coordenadas.
  public AsignacionDisponible(
      String idDonacion, String idEntidad, String razonSocial,
      String direccion, String telefono, String estadoDonacion
  ) {
    this(idDonacion, idEntidad, razonSocial, direccion, telefono,
        estadoDonacion, null, null);
  }

  public boolean tieneUbicacion() {
    return latitud != null && longitud != null
        && Double.isFinite(latitud) && Double.isFinite(longitud)
        && Math.abs(latitud) <= 90 && Math.abs(longitud) <= 180;
  }

  public List<Double> coordenadasParaHeiGit() {
    if (!tieneUbicacion()) {
      throw new IllegalStateException(
          "Falta confirmar la ubicacion de la entidad " + idEntidad
      );
    }
    return List.of(longitud, latitud);
  }
}