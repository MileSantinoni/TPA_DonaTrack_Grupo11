package org.example.dominio.logistica;

import java.time.LocalDateTime;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.jupiter.api.Test;
import org.example.Repositorios.RepositorioRutas;

import static org.junit.jupiter.api.Assertions.*;

class RutaPersistenciaTest {

  @Test
  void guardaYRecuperaRutaConDepositoYEntregas() {
    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory("logistica-test");
    EntityManager em = factory.createEntityManager();

    try {
      Camion camion = new Camion("AB123CD", 20, 2.5, 3500);

      Ruta ruta = new Ruta(camion, new UbicacionCamion(
          -34.60, -58.38, 0, LocalDateTime.now()
      ));

      // Las agregamos al revés para verificar el orden al recuperarlas.
      ruta.agregarEntrega(
          new Entrega("D2", "E2", "Hogar", "Calle 2", "456", 2)
      );
      ruta.agregarEntrega(
          new Entrega("D1", "E1", "Comedor", "Calle 1", "123", 1)
      );

      RepositorioRutas rutas = new RepositorioRutas(em);

      em.getTransaction().begin();
      em.persist(camion);
      rutas.agregar(ruta);
      em.getTransaction().commit();

      String idRuta = ruta.getId();

      // Descarta los objetos administrados: la siguiente lectura va a la BD.
      em.clear();

      Ruta recuperada = rutas.buscarPorId(idRuta).orElseThrow();

      assertNotNull(recuperada);
      assertEquals("AB123CD", recuperada.getCamion().getPatente());
      assertFalse(recuperada.estaActiva());

      assertNotNull(recuperada.getUbicacionDeposito());
      assertEquals(-34.60, recuperada.getUbicacionDeposito().getLatitud());
      assertEquals(-58.38, recuperada.getUbicacionDeposito().getLongitud());

      assertEquals(2, recuperada.cantidadEntregas());
      assertEquals(
          List.of("D1", "D2"),
          recuperada.getEntregas().stream()
              .map(Entrega::getIdDonacion)
              .toList()
      );

      Entrega primera = recuperada.getEntregas().get(0);
      assertEquals("E1", primera.getIdEntidad());
      assertEquals("Calle 1", primera.getDireccion());
      assertEquals(EstadoEntrega.PENDIENTE, primera.getEstado());

    } finally {
      try {
        if (em.getTransaction().isActive()) {
          em.getTransaction().rollback();
        }
      } finally {
        em.close();
        factory.close();
      }
    }
  }

  @Test
  void conservaEventoPendienteAlRecuperarRuta() throws Exception {
    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory("logistica-test");
    EntityManager em = factory.createEntityManager();

    try {
      RepositorioRutas rutas = new RepositorioRutas(em);
      Camion camion = new Camion("PEND001", 20, 2.5, 3500);
      Ruta ruta = new Ruta(camion);

      ruta.agregarEntrega(
          new Entrega("D1", "E1", "Comedor", "Calle 1", "123", 1)
      );

      // Capturamos el evento enviado antes de simular la pérdida de respuesta.
      var enviados = new java.util.ArrayList<EventoLogistico>();

      Donaciones conexionFallida = new DonacionesDePrueba() {
        @Override
        public void informar(EventoLogistico evento)
            throws java.io.IOException {
          enviados.add(evento);
          throw new java.io.IOException("Respuesta perdida");
        }
      };

      assertThrows(
          java.io.IOException.class,
          () -> ruta.iniciar(conexionFallida)
      );

      assertEquals(1, enviados.size());
      EventoLogistico original = enviados.get(0);
      assertFalse(ruta.estaActiva());

      em.getTransaction().begin();
      em.persist(camion);
      rutas.agregar(ruta);
      em.getTransaction().commit();

      String idRuta = ruta.getId();
      em.clear();

      Ruta recuperada = rutas.buscarPorId(idRuta).orElseThrow();
      assertNotSame(ruta, recuperada);

      Donaciones conexionRecuperada = new DonacionesDePrueba() {
        @Override
        public void informar(EventoLogistico evento) {
          enviados.add(evento);
        }
      };

      recuperada.iniciar(conexionRecuperada);
      rutas.actualizar(recuperada);

      // Debe reenviar exactamente el mismo evento, incluido su ID y fecha.
      assertEquals(2, enviados.size());
      assertEquals(original, enviados.get(1));

      em.clear();

      Ruta confirmada = rutas.buscarPorId(idRuta).orElseThrow();
      assertTrue(confirmada.estaActiva());
      assertEquals(
          EstadoEntrega.EN_TRASLADO,
          confirmada.getEntregas().get(0).getEstado()
      );

      // Una ruta ya iniciada no debe volver a informar el inicio.
      confirmada.iniciar(conexionRecuperada);
      assertEquals(2, enviados.size());

    } finally {
      try {
        if (em.getTransaction().isActive()) {
          em.getTransaction().rollback();
        }
      } finally {
        em.close();
        factory.close();
      }
    }
  }
}