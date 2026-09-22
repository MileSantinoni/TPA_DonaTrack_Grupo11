package org.example.dominio.logistica;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class TrazabilidadEntregaTest {
  DonacionesDePrueba donaciones = new DonacionesDePrueba();
  Camion camion = new Camion("ABC", 20, 3, 1000);
  Entrega entrega = new Entrega("D1", "E1", "Comedor", "Calle 1", "123", 1);
  Ruta ruta() { Ruta ruta = new Ruta(camion); ruta.agregarEntrega(entrega); return ruta; }

  @Test void confirmaRecepcionYRegistraFechaCamionYFotos() throws Exception {
    Ruta ruta = ruta(); ruta.iniciar(donaciones);
    entrega.confirmarRecepcion(camion, donaciones);
    entrega.agregarFotoRecepcion("foto.jpg");
    assertEquals(EstadoEntrega.ENTREGADA, entrega.getEstado());
    assertNotNull(entrega.getFechaRecepcion());
    assertSame(camion, entrega.getCamionResponsable());
    assertEquals(1, entrega.getFotosRecepcion().size());
    assertEquals(100, ruta.porcentajeAvance());
    assertEquals(EventoLogistico.Tipo.RECEPCION, donaciones.eventos.get(1).tipo());
    assertEquals(entrega.getFechaRecepcion(), donaciones.eventos.get(1).fecha());
  }
  @Test void validaEstadosYFotos() throws Exception {
    assertThrows(IllegalStateException.class, () -> entrega.confirmarRecepcion(camion, donaciones));
    assertThrows(IllegalStateException.class, () -> entrega.agregarFotoRecepcion("foto"));
    ruta().iniciar(donaciones);
    entrega.confirmarRecepcion(camion, donaciones);
    assertThrows(IllegalArgumentException.class, () -> entrega.agregarFotoRecepcion(" "));
    assertThrows(IllegalStateException.class, () -> entrega.marcarNoRecibida("Cerrado", camion, donaciones));
  }
  @Test void entregaFallidaPuedeRetornarSoloConMotivo() throws Exception {
    ruta().iniciar(donaciones);
    assertThrows(IllegalArgumentException.class, () -> entrega.marcarNoRecibida(" ", camion, donaciones));
    entrega.marcarNoRecibida("Cerrado", camion, donaciones);
    assertEquals(EstadoEntrega.NO_RECIBIDA, entrega.getEstado());
    assertTrue(entrega.fueResuelta());
    assertThrows(IllegalArgumentException.class, () -> entrega.registrarRetornoADeposito(null, camion, donaciones));
    entrega.registrarRetornoADeposito("Retorno al deposito", camion, donaciones);
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstado());
    assertEquals(EventoLogistico.Tipo.RETORNO_DEPOSITO, donaciones.eventos.get(2).tipo());
  }
  @Test void rechazoRemotoNoIniciaLaRutaNiMutaEntregas() {
    donaciones.rechazar = true;
    Ruta ruta = ruta();
    assertThrows(IllegalStateException.class, () -> ruta.iniciar(donaciones));
    assertFalse(ruta.estaActiva());
    assertEquals(EstadoEntrega.PENDIENTE, entrega.getEstado());
  }
  @Test void inicioReintentadoUsaElMismoEventoYNoSeRepiteSiYaEstaActiva() throws Exception {
    Ruta ruta = ruta(); donaciones.perderRespuesta = true;
    assertThrows(IOException.class, () -> ruta.iniciar(donaciones));
    assertFalse(ruta.estaActiva());
    donaciones.perderRespuesta = false;
    ruta.iniciar(donaciones); ruta.iniciar(donaciones);
    assertEquals(2, donaciones.eventos.size());
    assertEquals(donaciones.eventos.get(0), donaciones.eventos.get(1));
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstado());
  }
  @Test void recepcionReintentadaConservaIdYFechaYNoPermiteOtraOperacionPendiente() throws Exception {
    ruta().iniciar(donaciones); donaciones.perderRespuesta = true;
    assertThrows(IOException.class, () -> entrega.confirmarRecepcion(camion, donaciones));
    assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstado());
    assertThrows(IllegalStateException.class, () -> entrega.marcarNoRecibida("Cerrado", camion, donaciones));
    donaciones.perderRespuesta = false;
    entrega.confirmarRecepcion(camion, donaciones);
    assertEquals(donaciones.eventos.get(1), donaciones.eventos.get(2));
  }
  @Test void noIniciaRutaConDonacionRepetida() {
    Ruta ruta = ruta(); ruta.agregarEntrega(entrega);
    assertThrows(IllegalStateException.class, () -> ruta.iniciar(donaciones));
    assertTrue(donaciones.eventos.isEmpty());
    assertFalse(ruta.estaActiva());
  }
}
