package org.example.dominio.logistica;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import org.example.Repositorios.RepositorioCamiones;
import org.junit.jupiter.api.AfterEach;
import org.example.Repositorios.RepositorioRutas;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import static org.junit.jupiter.api.Assertions.*;

public class MonitoreoCamionesTest {

  private MonitorCamiones monitor;
  private Camion camion;
  private Ruta ruta;
  private EntityManagerFactory factory;
  private EntityManager em;
  private RepositorioCamiones repositorio;

  private final DonacionesDePrueba donaciones = new DonacionesDePrueba();

  @BeforeEach
  public void setUp() throws Exception {
    factory = Persistence.createEntityManagerFactory("logistica-test");
    em = factory.createEntityManager();
    repositorio = new RepositorioCamiones(em);
    monitor = new MonitorCamiones(new RepositorioRutas(em));

    camion = new Camion("AB123CD", 20.0, 2.5, 3500.0);
    repositorio.agregar(camion);

    ruta = new Ruta(camion);

    ruta.agregarEntrega(new Entrega("D1", "E1", "Comedor", "Calle 1", "123", 1));
    ruta.agregarEntrega(new Entrega("D2", "E2", "Hogar", "Calle 2", "456", 2));

    monitor.registrarRuta(ruta);

    em.clear();
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
    repositorio.agregar(camionConDeposito);
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
    assertTrue(
        monitor.iniciarRuta("AB123CD", donaciones, repositorio)
    );

// Descartamos los objetos administrados para verificar el estado guardado.
    em.clear();

    ReporteUbicacion reporte = new ReporteUbicacion(
        "AB123CD",
        -34.60,
        -58.38,
        45.0,
        LocalDateTime.now()
    );

    boolean procesado = monitor.recibirReporte(reporte, repositorio);

    assertTrue(procesado);

    em.clear();

    Camion recuperado = repositorio
        .buscarPorPatente("AB123CD")
        .orElseThrow();

    assertNotNull(recuperado.getUltimaUbicacion());
    assertEquals(-34.60, recuperado.getUltimaUbicacion().getLatitud());
    assertEquals(-58.38, recuperado.getUltimaUbicacion().getLongitud());
    assertEquals(45.0, recuperado.getUltimaUbicacion().getVelocidad());
  }

  @Test
  public void seRechazaElReporteDeUnaPatenteDesconocida() throws Exception {
    ruta.iniciar(donaciones);
    ReporteUbicacion reporte = new ReporteUbicacion("XX999XX", -34.60, -58.38, 45.0, LocalDateTime.now());
    assertFalse(monitor.recibirReporte(reporte, repositorio));
  }

  @Test
  public void seRechazaElReporteSiLaRutaNoEstaActiva() throws Exception {
    // La ruta no se inició, por lo que no debe aceptar ubicaciones
    ReporteUbicacion reporte = new ReporteUbicacion("AB123CD", -34.60, -58.38, 45.0, LocalDateTime.now());
    assertFalse(monitor.recibirReporte(reporte, repositorio));
  }

  @Test
  public void seRechazaElReporteConCoordenadasInvalidas() throws Exception {
    ruta.iniciar(donaciones);
    ReporteUbicacion reporte = new ReporteUbicacion("AB123CD", 200.0, -58.38, 45.0, LocalDateTime.now());
    assertFalse(monitor.recibirReporte(reporte, repositorio));
  }
  @AfterEach
  public void cerrarPersistencia() {
    try {
      if (em != null && em.isOpen()) {
        try {
          if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
          }
        } finally {
          em.close();
        }
      }
    } finally {
      if (factory != null && factory.isOpen()) {
        factory.close();
      }
    }
  }
}
