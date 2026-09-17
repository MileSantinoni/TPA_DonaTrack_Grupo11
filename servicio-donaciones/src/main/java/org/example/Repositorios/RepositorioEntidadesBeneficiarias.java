package org.example.Repositorios;

import org.example.dominio.beneficiario.EntidadBeneficiaria;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RepositorioEntidadesBeneficiarias {

  private static RepositorioEntidadesBeneficiarias instancia;
  private final List<EntidadBeneficiaria> entidades;

  // vamos con un singletonnnnnnn
  private RepositorioEntidadesBeneficiarias() {
    this.entidades = new ArrayList<>();
  }


  public static RepositorioEntidadesBeneficiarias getInstance() {
    if (instancia == null) {
      instancia = new RepositorioEntidadesBeneficiarias();
    }
    return instancia;
  }


  public void agregar(EntidadBeneficiaria entidad) {
    this.entidades.add(entidad);
  }


  public void eliminar(EntidadBeneficiaria entidad) {
    this.entidades.remove(entidad);
  }


  public List<EntidadBeneficiaria> buscarTodas() {
    return new ArrayList<>(entidades);
  }


  public Optional<EntidadBeneficiaria> buscarPorId(String id) {
    return entidades.stream()
        .filter(entidad -> entidad.getId().equals(id))
        .findFirst();
  }

  // util para test
  public void limpiar() {
    this.entidades.clear();
  }
}