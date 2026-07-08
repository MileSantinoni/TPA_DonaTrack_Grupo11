package org.example.dominio.donacion;

import java.util.List;

public class EstadoDonacionEntregaFallida extends EstadoDonacionBase {

  @Override
  public EstadoDonacion getNombre() {
    return EstadoDonacion.ENTREGA_FALLIDA;
  }

  @Override
  protected List<EstadoDonacion> getEstadosPermitidos() {
    return permitidos(
        EstadoDonacion.EN_DEPOSITO,
        EstadoDonacion.VENCIDA
    );
  }
}
