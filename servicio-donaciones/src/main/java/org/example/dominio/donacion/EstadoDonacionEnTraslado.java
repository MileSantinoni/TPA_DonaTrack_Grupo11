package org.example.dominio.donacion;

import java.util.List;

public class EstadoDonacionEnTraslado extends EstadoDonacionBase {

  @Override
  public EstadoDonacion getNombre() {
    return EstadoDonacion.EN_TRASLADO;
  }

  @Override
  protected List<EstadoDonacion> getEstadosPermitidos() {
    return permitidos(
        EstadoDonacion.ENTREGADA,
        EstadoDonacion.ENTREGA_FALLIDA,
        EstadoDonacion.VENCIDA
    );
  }
}
