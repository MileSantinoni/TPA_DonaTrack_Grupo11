package org.example.dominio.donacion;

import java.util.List;

public class EstadoDonacionListaParaEntregar extends EstadoDonacionBase {

  @Override
  public EstadoDonacion getNombre() {
    return EstadoDonacion.LISTA_PARA_ENTREGAR;
  }

  @Override
  protected List<EstadoDonacion> getEstadosPermitidos() {
    return permitidos(
        EstadoDonacion.EN_TRASLADO,
        EstadoDonacion.VENCIDA
    );
  }
}
