package org.example.dominio.logistica;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.example.Repositorios.RepositorioCamiones;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RepositorioCamionesTest {

  private RepositorioCamiones repositorio;

  @BeforeEach
  public void setUp() {
    repositorio = RepositorioCamiones.getInstance();
    repositorio.limpiar();
  }

  @Test
  public void puedeGuardarYBuscarCamionPorPatente() {
    Camion camion = new Camion("AB123CD", 20.0, 2.5, 3500.0);

    repositorio.agregar(camion);

    assertTrue(repositorio.buscarPorPatente("AB123CD").isPresent());
    assertEquals(camion, repositorio.buscarPorPatente("AB123CD").get());
  }

  @Test
  public void buscarTodosDevuelveLosCamionesRegistrados() {
    Camion camion = new Camion("AB123CD", 20.0, 2.5, 3500.0);

    repositorio.agregar(camion);

    assertEquals(1, repositorio.buscarTodos().size());
  }

  @Test
  public void unCamionNuevoEstaDisponible() {
    Camion camion = new Camion("AB123CD", 20.0, 2.5, 3500.0);

    assertTrue(camion.estaDisponible());
  }

  @Test
  public void buscarDisponiblesDevuelveSoloCamionesDisponibles() {
    Camion disponible = new Camion("AB123CD", 20.0, 2.5, 3500.0);
    Camion noDisponible = new Camion("CD456EF", 20.0, 2.5, 3500.0);
    noDisponible.marcarNoDisponible();

    repositorio.agregar(disponible);
    repositorio.agregar(noDisponible);

    assertEquals(1, repositorio.buscarDisponibles().size());
    assertEquals(disponible, repositorio.buscarDisponibles().get(0));
    assertFalse(noDisponible.estaDisponible());
  }
}
