package org.example.dominio.logistica;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.dominio.notificacion.Notificador;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrazabilidadEntregaTest {
  private final Notificador notificador = mock(Notificador.class);
  private final Camion camion = new Camion("AB123CD", 20, 3, 3000);

  private Entrega entregaLista() {
    Donacion donacion = new Donacion("Arroz", 10, "kg", null, null, null, null);
    donacion.cambiarEstado(EstadoDonacion.ASIGNACION_REALIZADA, "Asignacion confirmada");
    donacion.cambiarEstado(EstadoDonacion.LISTA_PARA_ENTREGAR, "Ruta planificada");
    return new Entrega(donacion, new EntidadBeneficiaria("Comedor", "Direccion", "123"), 1);
  }

  @Test
  void inicioActualizaTodasLasEntregasYNotificaUnaSolaVez() {
    Ruta ruta = new Ruta(camion);
    Entrega entrega = entregaLista();
    ruta.agregarEntrega(entrega);
    ruta.iniciar(notificador);
    ruta.iniciar(notificador);
    assertTrue(ruta.estaActiva());
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstado());
    assertEquals(EstadoDonacion.EN_TRASLADO, entrega.getDonacion().getEstadoActual());
    assertEquals(3, entrega.getDonacion().getHistorialEstados().size());
    verify(notificador, times(1)).notificarInicioRuta(ruta);
  }

  @Test
  void inicioInvalidoNoModificaLaRutaNiLasOtrasEntregas() {
    Ruta ruta = new Ruta(camion);
    Entrega primera = entregaLista();
    Entrega invalida = entregaLista();
    invalida.getDonacion().cambiarEstado(EstadoDonacion.VENCIDA, "Vencimiento");
    ruta.agregarEntrega(primera);
    ruta.agregarEntrega(invalida);
    assertThrows(IllegalStateException.class, () -> ruta.iniciar(notificador));
    assertFalse(ruta.estaActiva());
    assertEquals(EstadoEntrega.PENDIENTE, primera.getEstado());
    assertEquals(EstadoDonacion.LISTA_PARA_ENTREGAR, primera.getDonacion().getEstadoActual());
    verifyNoInteractions(notificador);
  }

  @Test
  void recepcionRegistraCamionFechaYPermiteFotosPosteriores() {
    Entrega entrega = entregaLista();
    entrega.iniciarTraslado();
    entrega.confirmarRecepcion(camion, notificador);
    assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstado());
    assertEquals(EstadoDonacion.ENTREGADA, entrega.getDonacion().getEstadoActual());
    assertSame(camion, entrega.getCamionResponsable());
    assertNotNull(entrega.getFechaRecepcion());
    assertTrue(entrega.getFotosRecepcion().isEmpty());
    entrega.agregarFotoRecepcion("foto.jpg");
    assertEquals(java.util.List.of("foto.jpg"), entrega.getFotosRecepcion());
    verify(notificador).notificarEntregaExitosa(entrega);
    assertThrows(IllegalStateException.class, () -> entrega.confirmarRecepcion(camion, notificador));
  }

  @Test
  void falloYRetornoConservanLosEstadosCoherentes() {
    Entrega entrega = entregaLista();
    entrega.iniciarTraslado();
    entrega.marcarNoRecibida("Nadie respondio", notificador);
    assertEquals(EstadoEntrega.NO_RECIBIDA, entrega.getEstado());
    assertEquals(EstadoDonacion.ENTREGA_FALLIDA, entrega.getDonacion().getEstadoActual());
    verify(notificador).notificarEntregaNoRecibida(entrega, "Nadie respondio");
    entrega.registrarRetornoADeposito("Recibida en deposito");
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstado());
    assertEquals(EstadoDonacion.EN_DEPOSITO, entrega.getDonacion().getEstadoActual());
    assertEquals(5, entrega.getDonacion().getHistorialEstados().size());
    assertThrows(IllegalStateException.class, entrega::iniciarTraslado);
  }

  @Test
  void transicionRechazadaNoDejaLaEntregaConfirmada() {
    Entrega entrega = entregaLista();
    entrega.iniciarTraslado();
    entrega.getDonacion().cambiarEstado(EstadoDonacion.VENCIDA, "Vencimiento");
    assertThrows(IllegalStateException.class, () -> entrega.confirmarRecepcion(camion, notificador));
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstado());
    assertNull(entrega.getFechaRecepcion());
    assertNull(entrega.getCamionResponsable());
    verifyNoInteractions(notificador);
  }

  @Test
  void motivoVacioYFotosAntesDeRecepcionSonRechazados() {
    Entrega entrega = entregaLista();
    entrega.iniciarTraslado();
    assertThrows(IllegalArgumentException.class, () -> entrega.marcarNoRecibida(" ", notificador));
    assertThrows(IllegalStateException.class, () -> entrega.agregarFotoRecepcion("foto.jpg"));
    assertEquals(EstadoDonacion.EN_TRASLADO, entrega.getDonacion().getEstadoActual());
    verifyNoInteractions(notificador);
  }
}
