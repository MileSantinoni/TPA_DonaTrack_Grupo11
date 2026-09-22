package org.example.dominio.logistica;

import org.junit.jupiter.api.*;
import org.example.Repositorios.RepositorioCamiones;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RegistroRutaTest {
  RepositorioCamiones camiones = RepositorioCamiones.getInstance();
  MonitorCamiones monitor = new MonitorCamiones();
  DonacionesDePrueba donaciones = new DonacionesDePrueba();
  Camion camion = new Camion("ABC", 20, 3, 1000);
  PlanDeRuta plan(String... ids) {
    return new PlanDeRuta("ABC", java.util.Arrays.stream(ids)
        .map(id -> new PlanDeRuta.Destino(id, "No confiar", "Direccion falsa", "000", 1)).toList(), null);
  }
  @BeforeEach void preparar() { camiones.limpiar(); camiones.agregar(camion); }
  @AfterEach void limpiar() { camiones.limpiar(); }

  @Test void registraConDatosRemotosYReservaCamion() throws Exception {
    assertTrue(monitor.registrarRuta(plan("D1"), camiones, donaciones));
    assertFalse(camion.estaDisponible());
    Entrega entrega = monitor.rutaDe("ABC").getEntregas().get(0);
    assertEquals("E1", entrega.getIdEntidad());
    assertEquals("Calle 1", entrega.getDireccion());
  }
  @Test void noRegistraCamionNoDisponible() throws Exception {
    camion.marcarNoDisponible();
    assertFalse(monitor.registrarRuta(plan("D1"), camiones, donaciones));
  }
  @Test void noReservaCamionSiFaltaUnaAsignacion() throws Exception {
    assertFalse(monitor.registrarRuta(plan("D1", "INEXISTENTE"), camiones, donaciones));
    assertNull(monitor.rutaDe("ABC"));
    assertTrue(camion.estaDisponible());
  }
  @Test void rechazaDonacionesRepetidasSinEfectosParciales() {
    assertThrows(IllegalStateException.class, () -> monitor.registrarRuta(plan("D1", "D1"), camiones, donaciones));
    assertNull(monitor.rutaDe("ABC"));
    assertTrue(camion.estaDisponible());
  }
}
