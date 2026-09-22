package org.example.dominio.logistica;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class MonitoreoCamionesTest {

  private MonitorCamiones monitor;
  private Camion camion;
  private Ruta ruta;

  private final DonacionesDePrueba donaciones = new DonacionesDePrueba();

  @BeforeEach
  public void setUp() throws Exception {
    monitor = new MonitorCamiones();
    camion = new Camion("AB123CD", 20.0, 2.5, 3500.0);
    ruta = new Ruta(camion);

    ruta.agregarEntrega(new Entrega("D1", "E1", "Comedor", "Calle 1", "123", 1));
    ruta.agregarEntrega(new Entrega("D2", "E2", "Hogar", "Calle 2", "456", 2));

    monitor.registrarRuta(ruta);
  }

  @Test
  public void alIniciarLaRutaLasEntregasPasanAEnTraslado() throws Exception {
    ruta.iniciar(donaciones);

    for (Entrega entrega : ruta.getEntregas()) {
      assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstado());
    }
  }

  @Test
  public void alIniciarRutaConUbicacionDepositoElCamionParteDesdeDeposito() throws Exception {
    Camion camionConDeposito = new Camion("CD456EF", 20.0, 2.5, 3500.0);
    UbicacionCamion ubicacionDeposito = new UbicacionCamion(
        -34.60,
        -58.38,
        0.0,
        LocalDateTime.now()
    );
    Ruta rutaConDeposito = new Ruta(camionConDeposito, ubicacionDeposito);

    rutaConDeposito.agregarEntrega(new Entrega("D1", "E1", "Comedor", "Calle 1", "123", 1));
    monitor.registrarRuta(rutaConDeposito);
    rutaConDeposito.iniciar(donaciones);

    UbicacionCamion ubicacionActual = monitor.ubicacionActual("CD456EF");

    assertEquals(-34.60, ubicacionActual.getLatitud());
    assertEquals(-58.38, ubicacionActual.getLongitud());
    assertEquals(0.0, ubicacionActual.getVelocidad());
  }

  @Test
  public void elAvanceReflejaLasEntregasCompletadas() throws Exception {
    ruta.iniciar(donaciones);
    ruta.getEntregas().get(0).confirmarRecepcion(camion, donaciones);
    assertEquals(50.0, ruta.porcentajeAvance());
  }

  @Test
  public void unReporteValidoActualizaLaUbicacionDelCamion() throws Exception {
    ruta.iniciar(donaciones);
    ReporteUbicacion reporte = new ReporteUbicacion("AB123CD", -34.60, -58.38, 45.0, LocalDateTime.now());

    boolean procesado = monitor.recibirReporte(reporte);

    assertTrue(procesado);
    assertEquals(-34.60, monitor.ubicacionActual("AB123CD").getLatitud());
  }

  @Test
  public void seRechazaElReporteDeUnaPatenteDesconocida() throws Exception {
    ruta.iniciar(donaciones);
    ReporteUbicacion reporte = new ReporteUbicacion("XX999XX", -34.60, -58.38, 45.0, LocalDateTime.now());
    assertFalse(monitor.recibirReporte(reporte));
  }

  @Test
  public void seRechazaElReporteSiLaRutaNoEstaActiva() throws Exception {
    // La ruta no se inició, por lo que no debe aceptar ubicaciones
    ReporteUbicacion reporte = new ReporteUbicacion("AB123CD", -34.60, -58.38, 45.0, LocalDateTime.now());
    assertFalse(monitor.recibirReporte(reporte));
  }

  @Test
  public void seRechazaElReporteConCoordenadasInvalidas() throws Exception {
    ruta.iniciar(donaciones);
    ReporteUbicacion reporte = new ReporteUbicacion("AB123CD", 200.0, -58.38, 45.0, LocalDateTime.now());
    assertFalse(monitor.recibirReporte(reporte));
  }
}
