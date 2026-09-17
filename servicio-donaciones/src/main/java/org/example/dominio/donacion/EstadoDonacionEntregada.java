package org.example.dominio.donacion;

import java.util.List;

public class EstadoDonacionEntregada extends EstadoDonacionBase {

  @Override
  public EstadoDonacion getNombre() {
    return EstadoDonacion.ENTREGADA;
  }

  @Override
  protected List<EstadoDonacion> getEstadosPermitidos() {
    return permitidos();
  }
}
