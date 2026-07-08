package org.example.dominio.donacion;

import java.util.List;

public class EstadoDonacionEnDeposito extends EstadoDonacionBase {

  @Override
  public EstadoDonacion getNombre() {
    return EstadoDonacion.EN_DEPOSITO;
  }

  @Override
  protected List<EstadoDonacion> getEstadosPermitidos() {
    return permitidos(
        EstadoDonacion.ASIGNACION_REALIZADA,
        EstadoDonacion.VENCIDA
    );
  }
}
