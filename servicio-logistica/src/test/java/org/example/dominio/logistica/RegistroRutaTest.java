package org.example.dominio.logistica;

import org.example.Repositorios.RepositorioCamiones;
import org.junit.jupiter.api.*;
import org.example.Repositorios.RepositorioRutas;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class RegistroRutaTest {

  private EntityManagerFactory factory;
  private EntityManager em;
  private RepositorioCamiones camiones;
  private MonitorCamiones monitor;
  private DonacionesDePrueba donaciones;
  private Camion camion;

  @BeforeEach
  void preparar() {
    factory = Persistence.createEntityManagerFactory("logistica-test");
    em = factory.createEntityManager();

    camiones = new RepositorioCamiones(em);
    monitor = new MonitorCamiones(new RepositorioRutas(em));
    donaciones = new DonacionesDePrueba();

    camion = new Camion("ABC", 20, 3, 1000);
    camiones.agregar(camion);
    em.clear();
  }

  @AfterEach
  void cerrar() {
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

  private PlanDeRuta plan(String... ids) {
    return new PlanDeRuta(
        "ABC",
        Arrays.stream(ids)
            .map(id -> new PlanDeRuta.Destino(
                id, "No confiar", "Direccion falsa", "000", 1
            ))
            .toList(),
        null
    );
  }

  private Camion leerCamionDesdeBase() {
    em.clear();
    return camiones.buscarPorPatente("ABC").orElseThrow();
  }

  @Test
  void registraConDatosRemotosYReservaCamion() throws Exception {
    assertTrue(
        monitor.registrarRuta(plan("D1"), camiones, donaciones)
    );

    assertFalse(leerCamionDesdeBase().estaDisponible());

    Entrega entrega = monitor.rutaDe("ABC").getEntregas().get(0);

    assertEquals("E1", entrega.getIdEntidad());
    assertEquals("Calle 1", entrega.getDireccion());
  }

  @Test
  void noRegistraCamionNoDisponible() throws Exception {
    camion.marcarNoDisponible();
    camiones.actualizar(camion);
    em.clear();

    assertFalse(
        monitor.registrarRuta(plan("D1"), camiones, donaciones)
    );

    assertNull(monitor.rutaDe("ABC"));
    assertFalse(leerCamionDesdeBase().estaDisponible());
  }

  @Test
  void noReservaCamionSiFaltaUnaAsignacion() throws Exception {
    assertFalse(
        monitor.registrarRuta(
            plan("D1", "INEXISTENTE"), camiones, donaciones
        )
    );

    assertNull(monitor.rutaDe("ABC"));
    assertTrue(leerCamionDesdeBase().estaDisponible());
  }

  @Test
  void rechazaDonacionesRepetidasSinEfectosParciales() {
    assertThrows(
        IllegalStateException.class,
        () -> monitor.registrarRuta(
            plan("D1", "D1"), camiones, donaciones
        )
    );

    assertNull(monitor.rutaDe("ABC"));
    assertTrue(leerCamionDesdeBase().estaDisponible());
  }
}