package org.example.dominio.logistica;

import java.util.UUID;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.example.service.NotificacionesService;
import org.example.service.TrazabilidadEntregaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class MonitoreoCamionesTest {

  private MonitorCamiones monitor;
  private Camion camion;
  private Ruta ruta;

  @BeforeEach
  public void setUp() {
    monitor = new MonitorCamiones();
    camion = new Camion("AB123CD", 20.0, 2.5, 3500.0);
    ruta = new Ruta(camion);

    EntidadBeneficiaria comedor = new EntidadBeneficiaria(
        "Comedor Sol",
        "Av. Siempreviva 742",
        "1140001111"
    );

    EntidadBeneficiaria hogar = new EntidadBeneficiaria(
        "Hogar Luz",
        "Calle Falsa 123",
        "1140002222"
    );

    Subcategoria subcategoria = new Subcategoria(
        UUID.randomUUID().toString(),
        "Alimentos",
        TipoAtributo.PERECEDERO // o el que corresponda
    );

    Donante donante = new PersonaHumana(
        "donante@test.com",
        "12345678",
        TipoDocumento.DNI,
        "Juan",
        "Perez",
        30,
        Genero.MASCULINO,
        "Calle 123"
    );

    Donacion donacion1 = new Donacion(
        "Fideos",
        100,
        "Paquetes",
        subcategoria,
        null,
        null,
        donante
    );

    Donacion donacion2 = new Donacion(
        "Arroz",
        50,
        "Paquetes",
        subcategoria,
        null,
        null,
        donante
    );

    donacion1.cambiarEstado(
        EstadoDonacion.ASIGNACION_REALIZADA,
        "Asignada a entidad beneficiaria"
    );

    donacion1.cambiarEstado(
        EstadoDonacion.LISTA_PARA_ENTREGAR,
        "Incluida en una ruta planificada"
    );

    donacion2.cambiarEstado(
        EstadoDonacion.ASIGNACION_REALIZADA,
        "Asignada a entidad beneficiaria"
    );

    donacion2.cambiarEstado(
        EstadoDonacion.LISTA_PARA_ENTREGAR,
        "Incluida en una ruta planificada"
    );

    ruta.agregarEntrega(new Entrega(donacion1, comedor, 1));
    ruta.agregarEntrega(new Entrega(donacion2, hogar, 2));

    monitor.registrarRuta(ruta);
  }

  @Test
  public void alIniciarLaRutaLasEntregasPasanAEnTraslado() {
    TrazabilidadEntregaService trazabilidadService =
        new TrazabilidadEntregaService(new NotificacionesService());

    trazabilidadService.iniciarRuta(ruta);

    for (Entrega entrega : ruta.getEntregas()) {
      assertEquals(EstadoEntrega.EN_TRASLADO, entrega.getEstado());
    }
  }

  @Test
  public void elAvanceReflejaLasEntregasCompletadas() {
    ruta.iniciar();
    ruta.getEntregas().get(0).marcarEntregada(camion);
    assertEquals(50.0, ruta.porcentajeAvance());
  }

  @Test
  public void unReporteValidoActualizaLaUbicacionDelCamion() {
    ruta.iniciar();
    ReporteUbicacion reporte = new ReporteUbicacion("AB123CD", -34.60, -58.38, 45.0, LocalDateTime.now());

    boolean procesado = monitor.recibirReporte(reporte);

    assertTrue(procesado);
    assertEquals(-34.60, monitor.ubicacionActual("AB123CD").getLatitud());
  }

  @Test
  public void seRechazaElReporteDeUnaPatenteDesconocida() {
    ruta.iniciar();
    ReporteUbicacion reporte = new ReporteUbicacion("XX999XX", -34.60, -58.38, 45.0, LocalDateTime.now());
    assertFalse(monitor.recibirReporte(reporte));
  }

  @Test
  public void seRechazaElReporteSiLaRutaNoEstaActiva() {
    // La ruta no se inició, por lo que no debe aceptar ubicaciones
    ReporteUbicacion reporte = new ReporteUbicacion("AB123CD", -34.60, -58.38, 45.0, LocalDateTime.now());
    assertFalse(monitor.recibirReporte(reporte));
  }

  @Test
  public void seRechazaElReporteConCoordenadasInvalidas() {
    ruta.iniciar();
    ReporteUbicacion reporte = new ReporteUbicacion("AB123CD", 200.0, -58.38, 45.0, LocalDateTime.now());
    assertFalse(monitor.recibirReporte(reporte));
  }
}