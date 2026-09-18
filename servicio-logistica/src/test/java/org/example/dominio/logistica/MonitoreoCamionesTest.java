package org.example.dominio.logistica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MonitoreoCamionesTest {
  private MonitorCamiones monitor;
  private Camion camion;
  private Ruta ruta;

  @BeforeEach
  void preparar() {
    monitor = new MonitorCamiones();
    camion = new Camion("AB123CD", 20, 3, 3500);
    ruta = new Ruta(camion);
    ruta.agregarEntrega(new Entrega("DON-1", null, "Comedor", "Calle 1", "111", 1));
    ruta.agregarEntrega(new Entrega("DON-2", null, "Hogar", "Calle 2", "222", 2));
    monitor.registrarRuta(ruta);
  }

  @Test
  void iniciarActivaLaRutaSinCambiarLasEntregas() {
    assertTrue(monitor.iniciarRuta("AB123CD"));
    assertTrue(ruta.estaActiva());
    assertEquals(EstadoEntrega.PENDIENTE, ruta.getEntregas().get(0).getEstado());
  }

  @Test
  void iniciarConDepositoEstableceLaUbicacionInicial() {
    Camion otro = new Camion("CD456EF", 20, 3, 3500);
    UbicacionCamion deposito = new UbicacionCamion(-34.60, -58.38, 0,
        LocalDateTime.now());
    monitor.registrarRuta(new Ruta(otro, deposito));
    assertTrue(monitor.iniciarRuta("CD456EF"));
    assertEquals(-34.60, monitor.ubicacionActual("CD456EF").getLatitud());
  }

  @Test
  void avanceCuentaEntregasCompletadas() {
    ruta.getEntregas().get(0).marcarEntregada(camion);
    assertEquals(50.0, monitor.avanceDeRuta("AB123CD"));
  }

  @Test
  void reporteValidoActualizaUbicacion() {
    monitor.iniciarRuta("AB123CD");
    assertTrue(monitor.recibirReporte(reporte("AB123CD", -34.60)));
    assertEquals(-34.60, monitor.ubicacionActual("AB123CD").getLatitud());
  }

  @Test
  void rechazaPatenteDesconocida() {
    monitor.iniciarRuta("AB123CD");
    assertFalse(monitor.recibirReporte(reporte("XX999XX", -34.60)));
  }

  @Test
  void rechazaReporteAntesDeIniciar() {
    assertFalse(monitor.recibirReporte(reporte("AB123CD", -34.60)));
  }

  @Test
  void rechazaCoordenadasInvalidas() {
    monitor.iniciarRuta("AB123CD");
    assertFalse(monitor.recibirReporte(reporte("AB123CD", 200)));
  }

  private ReporteUbicacion reporte(String patente, double latitud) {
    return new ReporteUbicacion(patente, latitud, -58.38, 45, LocalDateTime.now());
  }
}
