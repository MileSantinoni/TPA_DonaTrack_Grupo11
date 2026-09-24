package org.example.dominio.logistica;

import org.junit.jupiter.api.Test;
import org.example.Repositorios.RepositorioCamiones;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CamionPersistenciaTest {

  @Test
  void guardaYRecuperaCamionConSuUbicacion() {
    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory("logistica-test");

    EntityManager em = factory.createEntityManager();

    try {
      Camion camion = new Camion("AB123CD", 20, 3, 1500);

      camion.actualizarUbicacion(new UbicacionCamion(
          -34.60,
          -58.38,
          45,
          LocalDateTime.of(2026, 9, 24, 10, 30)
      ));

      em.getTransaction().begin();
      em.persist(camion);
      em.getTransaction().commit();

      // Vaciar el contexto para leer desde la base.
      em.clear();

      Camion recuperado = em.find(Camion.class, "AB123CD");

      assertNotNull(recuperado);
      assertEquals(20.0, recuperado.getCapacidadVolumen());
      assertTrue(recuperado.estaDisponible());

      assertNotNull(recuperado.getUltimaUbicacion());
      assertEquals(
          -34.60,
          recuperado.getUltimaUbicacion().getLatitud()
      );
      assertEquals(
          45.0,
          recuperado.getUltimaUbicacion().getVelocidad()
      );
    } finally {
      if (em.getTransaction().isActive()) {
        em.getTransaction().rollback();
      }
      em.close();
      factory.close();
    }
  }

  @Test
  void repositorioGuardaActualizaYEliminaCamiones() {
    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory("logistica-test");

    EntityManager em = factory.createEntityManager();

    try {
      RepositorioCamiones repositorio = new RepositorioCamiones(em);

      Camion camion = new Camion("CD456EF", 25, 3, 2000);
      camion.actualizarUbicacion(new UbicacionCamion(
          -34.60, -58.38, 0,
          LocalDateTime.of(2026, 9, 24, 12, 0)
      ));

      repositorio.agregar(camion);
      em.clear();

      Camion recuperado = repositorio
          .buscarPorPatente("CD456EF")
          .orElseThrow();

      assertEquals(1, repositorio.buscarTodos().size());
      assertEquals(1, repositorio.buscarDisponibles().size());

      // Desvincularlo para comprobar que actualizar() usa merge.
      em.clear();

      recuperado.marcarNoDisponible();
      recuperado.actualizarUbicacion(new UbicacionCamion(
          -34.61, -58.39, 40,
          LocalDateTime.of(2026, 9, 24, 12, 5)
      ));

      repositorio.actualizar(recuperado);
      em.clear();

      Camion actualizado = repositorio
          .buscarPorPatente("CD456EF")
          .orElseThrow();

      assertFalse(actualizado.estaDisponible());
      assertTrue(repositorio.buscarDisponibles().isEmpty());
      assertEquals(
          -34.61,
          actualizado.getUltimaUbicacion().getLatitud()
      );

      // Los reportes actualizan una única fila de ubicación.
      assertEquals(
          1L,
          em.createQuery(
              "SELECT COUNT(u) FROM UbicacionCamion u", Long.class
          ).getSingleResult().longValue()
      );

      repositorio.eliminar(actualizado);
      em.clear();

      assertTrue(repositorio.buscarPorPatente("CD456EF").isEmpty());

      // La ubicación se elimina junto con el camión.
      assertEquals(
          0L,
          em.createQuery(
              "SELECT COUNT(u) FROM UbicacionCamion u", Long.class
          ).getSingleResult().longValue()
      );
    } finally {
      if (em.getTransaction().isActive()) {
        em.getTransaction().rollback();
      }
      em.close();
      factory.close();
    }
  }
}