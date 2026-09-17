package org.example.dominio.donacion;

import java.util.Arrays;
import java.util.List;

public abstract class EstadoDonacionBase implements EstadoDonacionState {

  @Override
  public void cambiarA(Donacion donacion, EstadoDonacion nuevoEstado, String justificativo) {
    if (!getEstadosPermitidos().contains(nuevoEstado)) {
      throw new IllegalStateException("No se puede cambiar una donacion de "
          + getNombre() + " a " + nuevoEstado);
    }

    donacion.aplicarCambioEstado(EstadoDonacionFactory.crear(nuevoEstado), justificativo);
  }

  protected List<EstadoDonacion> permitidos(EstadoDonacion... estados) {
    return Arrays.asList(estados);
  }

  protected abstract List<EstadoDonacion> getEstadosPermitidos();
}
