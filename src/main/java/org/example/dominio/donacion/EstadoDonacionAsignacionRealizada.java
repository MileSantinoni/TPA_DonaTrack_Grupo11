package org.example.dominio.donacion;

import java.util.List;

public class EstadoDonacionAsignacionRealizada extends EstadoDonacionBase {

  @Override
  public EstadoDonacion getNombre() {
    return EstadoDonacion.ASIGNACION_REALIZADA;
  }

  @Override
  protected List<EstadoDonacion> getEstadosPermitidos() {
    return permitidos(
        EstadoDonacion.LISTA_PARA_ENTREGAR,
        EstadoDonacion.VENCIDA
    );
  }
}
