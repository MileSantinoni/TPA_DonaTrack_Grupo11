package org.example.dominio.donacion;

public class EstadoDonacionFactory {

  private EstadoDonacionFactory() {
  }

  public static EstadoDonacionState crear(EstadoDonacion estado) {
    switch (estado) {
      case EN_DEPOSITO:
        return new EstadoDonacionEnDeposito();
      case ASIGNACION_REALIZADA:
        return new EstadoDonacionAsignacionRealizada();
      case LISTA_PARA_ENTREGAR:
        return new EstadoDonacionListaParaEntregar();
      case EN_TRASLADO:
        return new EstadoDonacionEnTraslado();
      case ENTREGADA:
        return new EstadoDonacionEntregada();
      case ENTREGA_FALLIDA:
        return new EstadoDonacionEntregaFallida();
      case VENCIDA:
        return new EstadoDonacionVencida();
      default:
        throw new IllegalArgumentException("Estado de donacion desconocido: " + estado);
    }
  }
}
