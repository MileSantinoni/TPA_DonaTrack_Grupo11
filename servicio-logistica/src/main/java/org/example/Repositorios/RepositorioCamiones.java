package org.example.Repositorios;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.example.dominio.logistica.Camion;

public class RepositorioCamiones {

  private static RepositorioCamiones instancia;
  private final List<Camion> camiones;

  private RepositorioCamiones() {
    this.camiones = new ArrayList<>();
  }

  public static RepositorioCamiones getInstance() {
    if (instancia == null) {
      instancia = new RepositorioCamiones();
    }
    return instancia;
  }

  public void agregar(Camion camion) {
    this.camiones.add(camion);
  }

  public void eliminar(Camion camion) {
    this.camiones.remove(camion);
  }

  public List<Camion> buscarTodos() {
    return new ArrayList<>(camiones);
  }

  public List<Camion> buscarDisponibles() {
    return camiones.stream()
        .filter(Camion::estaDisponible)
        .toList();
  }

  public Optional<Camion> buscarPorPatente(String patente) {
    return camiones.stream()
        .filter(camion -> camion.getPatente().equals(patente))
        .findFirst();
  }

  public void limpiar() {
    this.camiones.clear();
  }
}
