package org.example.Repositorios;

import org.example.dominio.algoritmo.ResultadoEjecucionAlgoritmos;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepositorioResultadosAlgoritmos {
  private static RepositorioResultadosAlgoritmos instancia;
  private final List<ResultadoEjecucionAlgoritmos> resultados;

  private RepositorioResultadosAlgoritmos() {
    this.resultados = new ArrayList<>();
  }

  public static RepositorioResultadosAlgoritmos getInstance() {
    if (instancia == null) {
      instancia = new RepositorioResultadosAlgoritmos();
    }
    return instancia;
  }

  public void agregar(ResultadoEjecucionAlgoritmos resultado) {
    this.resultados.add(resultado);
  }

  public List<ResultadoEjecucionAlgoritmos> buscarTodos() {
    return new ArrayList<>(resultados);
  }

  public Optional<ResultadoEjecucionAlgoritmos> buscarPorId(String id) {
    return resultados.stream()
        .filter(resultado -> resultado.getId().equals(id))
        .findFirst();
  }

  public List<ResultadoEjecucionAlgoritmos> buscarPorDonacion(String idDonacion) {
    return resultados.stream()
        .filter(resultado -> resultado.getDonacion().getId().equals(idDonacion))
        .toList();
  }

  public void eliminarPorDonacion(String idDonacion) {
    this.resultados.removeIf(resultado -> resultado.getDonacion().getId().equals(idDonacion));
  }

  public void limpiar() {
    this.resultados.clear();
  }
}
