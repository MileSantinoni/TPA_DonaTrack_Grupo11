package org.example.Repositorios;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.example.dominio.beneficiario.EntidadBeneficiaria;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class RepositorioEntidadesBeneficiarias implements WithSimplePersistenceUnit {

  private static final RepositorioEntidadesBeneficiarias INSTANCE = new RepositorioEntidadesBeneficiarias();

  public static RepositorioEntidadesBeneficiarias getInstance() {
    return INSTANCE;
  }

  public void agregar(EntidadBeneficiaria entidad) {
    entityManager().getTransaction().begin();
    entityManager().persist(entidad);
    entityManager().getTransaction().commit();
  }

  public List<EntidadBeneficiaria> buscarTodos() {
    return entityManager()
        .createQuery("from EntidadBeneficiaria", EntidadBeneficiaria.class)
        .getResultList();
  }

  public List<EntidadBeneficiaria> buscarTodas() {
    return buscarTodos();
  }

  public Optional<EntidadBeneficiaria> buscarPorId(String idStr) {
    try {
      UUID id = UUID.fromString(idStr);
      return Optional.ofNullable(entityManager().find(EntidadBeneficiaria.class, id));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public void eliminar(EntidadBeneficiaria entidad) {
    entityManager().getTransaction().begin();
    // Aseguramos que la entidad esté asociada al EntityManager antes de borrarla
    EntidadBeneficiaria aBorrar = entityManager().contains(entidad) ? entidad : entityManager().merge(entidad);
    entityManager().remove(aBorrar);
    entityManager().getTransaction().commit();
  }

  public void limpiar() {
    entityManager().getTransaction().begin();
    entityManager().createQuery("DELETE FROM EntidadBeneficiaria").executeUpdate();
    entityManager().getTransaction().commit();
  }
}