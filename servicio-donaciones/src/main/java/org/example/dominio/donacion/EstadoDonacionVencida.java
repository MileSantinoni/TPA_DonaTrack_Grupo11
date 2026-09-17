package org.example.dominio.donacion;

import java.util.List;

public class EstadoDonacionVencida extends EstadoDonacionBase {

  @Override
  public EstadoDonacion getNombre() {
    return EstadoDonacion.VENCIDA;
  }

  @Override
  protected List<EstadoDonacion> getEstadosPermitidos() {
    return permitidos();
  }
}
