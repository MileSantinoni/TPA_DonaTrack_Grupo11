package org.example.Repositorios;

import org.example.dominio.donacion.Donacion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepositorioDonaciones {

  private static RepositorioDonaciones instancia;
  private final List<Donacion> donaciones;

  private RepositorioDonaciones() {
    this.donaciones = new ArrayList<>();
  }

  public static RepositorioDonaciones getInstance() {
    if (instancia == null) {
      instancia = new RepositorioDonaciones();
    }
    return instancia;
  }

  public void agregar(Donacion donacion) {
    this.donaciones.add(donacion);
  }

  public void eliminar(Donacion donacion) {
    this.donaciones.remove(donacion);
  }

  public List<Donacion> buscarTodas() {
    return new ArrayList<>(donaciones);
  }

  public Optional<Donacion> buscarPorId(String id) {
    return donaciones.stream()
        .filter(d -> d.getId().equals(id))
        .findFirst();
  }

  public void limpiar() {
    this.donaciones.clear();
  }
}
