package org.example.dominio.logistica;

import org.example.Repositorios.RepositorioCamiones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RepositorioCamionesTest {

  private EntityManagerFactory factory;
  private EntityManager em;
  private RepositorioCamiones repositorio;

  @BeforeEach
  public void setUp() {
    factory = Persistence.createEntityManagerFactory("logistica-test");
    em = factory.createEntityManager();
    repositorio = new RepositorioCamiones(em);
  }

  @AfterEach
  public void cerrar() {
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

  @Test
  public void puedeGuardarYBuscarCamionPorPatente() {
    Camion camion = new Camion("AB123CD", 20.0, 2.5, 3500.0);

    repositorio.agregar(camion);
    em.clear();

    Camion recuperado = repositorio
        .buscarPorPatente("AB123CD")
        .orElseThrow();

    assertEquals("AB123CD", recuperado.getPatente());
    assertEquals(20.0, recuperado.getCapacidadVolumen());
    assertEquals(2.5, recuperado.getAltura());
    assertEquals(3500.0, recuperado.getCapacidadCarga());
  }

  @Test
  public void buscarTodosDevuelveLosCamionesRegistrados() {
    repositorio.agregar(
        new Camion("AB123CD", 20.0, 2.5, 3500.0)
    );
    em.clear();

    List<Camion> encontrados = repositorio.buscarTodos();

    assertEquals(1, encontrados.size());
    assertEquals("AB123CD", encontrados.get(0).getPatente());
  }

  @Test
  public void unCamionNuevoEstaDisponible() {
    Camion camion = new Camion("AB123CD", 20.0, 2.5, 3500.0);

    assertTrue(camion.estaDisponible());
  }

  @Test
  public void buscarDisponiblesDevuelveSoloCamionesDisponibles() {
    Camion disponible =
        new Camion("AB123CD", 20.0, 2.5, 3500.0);

    Camion noDisponible =
        new Camion("CD456EF", 20.0, 2.5, 3500.0);
    noDisponible.marcarNoDisponible();

    repositorio.agregar(disponible);
    repositorio.agregar(noDisponible);
    em.clear();

    List<Camion> disponibles = repositorio.buscarDisponibles();

    assertEquals(1, disponibles.size());
    assertEquals("AB123CD", disponibles.get(0).getPatente());

    assertFalse(
        repositorio.buscarPorPatente("CD456EF")
            .orElseThrow()
            .estaDisponible()
    );
  }
}