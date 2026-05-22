package org.example.dominio.donacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.example.dominio.catalogo.Subcategoria;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

public class DonacionTest {
  @Test
  void unaDonacionPuedeQuedarEntregada() {
    Subcategoria fideos = new Subcategoria("Fideos");

    Donacion donacion = new Donacion(
        "Paquetes de fideos",
        100,
        "PAQUETE",
        fideos,
        LocalDate.of(2027, 1, 1),
        null
    );

    donacion.cambiarEstado(
        EstadoDonacion.ENTREGADA,
        "La entidad beneficiaria confirmó la recepción"
    );

    assertEquals(EstadoDonacion.ENTREGADA, donacion.getEstadoActual());
  }
}
