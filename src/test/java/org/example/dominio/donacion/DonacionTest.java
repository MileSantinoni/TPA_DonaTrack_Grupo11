package org.example.dominio.donacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

public class DonacionTest {

  @Test
  void unaDonacionPuedeQuedarEntregadaSiguiendoElCicloDeEstados() {
    Donacion donacion = crearDonacion();

    donacion.cambiarEstado(EstadoDonacion.ASIGNACION_REALIZADA, "Se asigno a una entidad");
    donacion.cambiarEstado(EstadoDonacion.LISTA_PARA_ENTREGAR, "La donacion esta preparada");
    donacion.cambiarEstado(EstadoDonacion.EN_TRASLADO, "La donacion salio del deposito");
    donacion.cambiarEstado(EstadoDonacion.ENTREGADA, "La entidad beneficiaria confirmo la recepcion");

    assertEquals(EstadoDonacion.ENTREGADA, donacion.getEstadoActual());
  }

  @Test
  void unaDonacionNoPuedeQuedarEntregadaDirectamenteDesdeDeposito() {
    Donacion donacion = crearDonacion();

    assertThrows(
        IllegalStateException.class,
        () -> donacion.cambiarEstado(EstadoDonacion.ENTREGADA, "Intento de entrega directa")
    );
  }

  @Test
  void unaEntregaFallidaPuedePasarAVencida() {
    Donacion donacion = crearDonacion();

    donacion.cambiarEstado(EstadoDonacion.ASIGNACION_REALIZADA, "Se asigno a una entidad");
    donacion.cambiarEstado(EstadoDonacion.LISTA_PARA_ENTREGAR, "La donacion esta preparada");
    donacion.cambiarEstado(EstadoDonacion.EN_TRASLADO, "La donacion salio del deposito");
    donacion.cambiarEstado(EstadoDonacion.ENTREGA_FALLIDA, "No se pudo entregar");
    donacion.cambiarEstado(EstadoDonacion.VENCIDA, "La donacion vencio");

    assertEquals(EstadoDonacion.VENCIDA, donacion.getEstadoActual());
  }

  @Test
  void unaEntregaFallidaPuedeVolverADeposito() {
    Donacion donacion = crearDonacion();

    donacion.cambiarEstado(EstadoDonacion.ASIGNACION_REALIZADA, "Se asigno a una entidad");
    donacion.cambiarEstado(EstadoDonacion.LISTA_PARA_ENTREGAR, "La donacion esta preparada");
    donacion.cambiarEstado(EstadoDonacion.EN_TRASLADO, "La donacion salio del deposito");
    donacion.cambiarEstado(EstadoDonacion.ENTREGA_FALLIDA, "No se pudo entregar");
    donacion.cambiarEstado(EstadoDonacion.EN_DEPOSITO, "La donacion vuelve al deposito");

    assertEquals(EstadoDonacion.EN_DEPOSITO, donacion.getEstadoActual());
  }

  private Donacion crearDonacion() {
    Subcategoria fideos = new Subcategoria("SUB-1", "Fideos", TipoAtributo.PERECEDERO);

    return new Donacion(
        "Paquetes de fideos",
        100,
        "PAQUETE",
        fideos,
        LocalDate.of(2027, 1, 1),
        null
    );
  }
}
