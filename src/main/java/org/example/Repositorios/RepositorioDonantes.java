package org.example.Repositorios;

import org.example.dominio.donante.Donante;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepositorioDonantes {

  private static RepositorioDonantes instancia;

  private final List<Donante> donantes;

  private RepositorioDonantes() {
    this.donantes = new ArrayList<>();
  }

  public static RepositorioDonantes getInstance() {
    if (instancia == null) {
      instancia = new RepositorioDonantes();
    }

    return instancia;
  }

  public void agregar(Donante donante) {
    this.donantes.add(donante);
  }

  public void eliminar(Donante donante) {
    this.donantes.remove(donante);
  }

  public List<Donante> buscarTodos() {
    return new ArrayList<>(donantes);
  }

  public Optional<Donante> buscarPorId(String id) {
    return donantes.stream()
        .filter(donante -> donante.getId().equals(id))
        .findFirst();
  }

  public Optional<Donante> buscarPorMail(String mail) {
    return donantes.stream()
        .filter(donante -> donante.getMail().equals(mail))
        .findFirst();
  }

  public boolean existeMail(String mail) {
    return buscarPorMail(mail).isPresent();
  }

  public void limpiar() {
    this.donantes.clear();
  }
}