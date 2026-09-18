package org.example.dominio.logistica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TrazabilidadEntregaTest {
  private final Camion camion = new Camion("AB123CD", 20, 3, 3000);

  private Entrega entrega() {
    return new Entrega("DON-1", "ENT-1", "Comedor", "Direccion", "123", 1);
  }

  @Test
  void conservaLosDatosDelDestinoRecibidosDeDonaciones() {
    Entrega entrega = entrega();
    assertEquals("DON-1", entrega.getIdDonacion());
    assertEquals("ENT-1", entrega.getIdEntidad());
    assertEquals("Comedor", entrega.getRazonSocial());
    assertEquals("Direccion", entrega.getDireccion());
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstado());
  }

  @Test
  void registraElCamionYLasFotosAlEntregar() {
    Entrega entrega = entrega();
    entrega.marcarEnTraslado();
    entrega.marcarEntregada(camion);
    entrega.agregarFotoRecepcion("foto.jpg");
    assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstado());
    assertSame(camion, entrega.getCamionResponsable());
    assertEquals(List.of("foto.jpg"), entrega.getFotosRecepcion());
    assertTrue(entrega.fueResuelta());
  }

  @Test
  void noRecibidaQuedaResuelta() {
    Entrega entrega = entrega();
    entrega.marcarNoRecibida();
    assertEquals(EstadoEntrega.NO_RECIBIDA, entrega.getEstado());
    assertTrue(entrega.fueResuelta());
  }

  @Test
  void retornoAlDepositoVuelveAPendiente() {
    Entrega entrega = entrega();
    entrega.marcarNoRecibida();
    entrega.volverAPendiente();
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstado());
    assertFalse(entrega.fueResuelta());
  }
}
