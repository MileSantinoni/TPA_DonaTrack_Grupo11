package org.example.Repositorios;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.catalogo.Subcategoria;

public class RepositorioEntidadesBeneficiarias
    implements WithSimplePersistenceUnit {

  private static final RepositorioEntidadesBeneficiarias INSTANCE =
      new RepositorioEntidadesBeneficiarias();

  private final Supplier<EntityManager> proveedor;

  public RepositorioEntidadesBeneficiarias() {
    this.proveedor = this::entityManager;
  }

  public RepositorioEntidadesBeneficiarias(EntityManager em) {
    Objects.requireNonNull(em);
    this.proveedor = () -> em;
  }

  public RepositorioEntidadesBeneficiarias(
      Supplier<EntityManager> proveedor
  ) {
    this.proveedor = Objects.requireNonNull(proveedor);
  }

  public static RepositorioEntidadesBeneficiarias getInstance() {
    return INSTANCE;
  }

  private EntityManager em() {
    EntityManager actual = proveedor.get();

    if (actual == null || !actual.isOpen()) {
      throw new IllegalStateException(
          "No hay un EntityManager abierto para esta operacion"
      );
    }

    return actual;
  }

  public void agregar(EntidadBeneficiaria entidad) {
    EntityManager em = em();
    enTransaccion(em, () -> {
      for (org.example.dominio.beneficiario.Necesidad nec : entidad.getNecesidades()) {
        if (nec.getSubcategoria() != null) {
          Subcategoria sub = em.find(Subcategoria.class, nec.getSubcategoria().getId());
          if (sub == null) {
            em.persist(nec.getSubcategoria());
          } else if (sub != nec.getSubcategoria()) {
            nec.setSubcategoria(sub);
          }
        }
      }
      em.persist(entidad);
    });
  }

  public void actualizar(EntidadBeneficiaria entidad) {
    EntityManager em = em();
    enTransaccion(em, () -> em.merge(entidad));
  }

  public List<EntidadBeneficiaria> buscarTodos() {
    return em().createQuery(
        "SELECT e FROM EntidadBeneficiaria e",
        EntidadBeneficiaria.class
    ).getResultList();
  }

  public List<EntidadBeneficiaria> buscarTodas() {
    return buscarTodos();
  }

  public Optional<EntidadBeneficiaria> buscarPorId(String idTexto) {
    if (idTexto == null || idTexto.isBlank()) {
      return Optional.empty();
    }

    UUID id;
    try {
      id = UUID.fromString(idTexto);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }

    return Optional.ofNullable(
        em().find(EntidadBeneficiaria.class, id)
    );
  }

  public void eliminar(EntidadBeneficiaria entidad) {
    if (entidad.getId() == null) {
      return;
    }

    EntityManager em = em();

    enTransaccion(em, () -> {
      EntidadBeneficiaria encontrada =
          em.find(EntidadBeneficiaria.class, entidad.getId());

      if (encontrada != null) {
        em.remove(encontrada);
      }
    });
  }

  // Utilizar únicamente sobre la base de pruebas.
  public void limpiar() {
    EntityManager em = em();

    enTransaccion(em, () -> {
      em.createNativeQuery("DELETE FROM asignaciones_donacion").executeUpdate();
      em.createNativeQuery("DELETE FROM representantes_beneficiarios").executeUpdate();
      em.createNativeQuery("DELETE FROM necesidades_recurrentes").executeUpdate();
      em.createNativeQuery("DELETE FROM necesidades_extraordinarias").executeUpdate();
      em.createNativeQuery("DELETE FROM necesidades").executeUpdate();
      em.createNativeQuery("DELETE FROM entidades_beneficiarias").executeUpdate();
    });
    em.clear();
  }

  private void enTransaccion(EntityManager em, Runnable operacion) {
    EntityTransaction tx = em.getTransaction();
    boolean propia = !tx.isActive();

    try {
      if (propia) {
        tx.begin();
      }

      operacion.run();

      if (propia) {
        tx.commit();
      }
    } catch (RuntimeException e) {
      if (tx.isActive()) {
        if (propia) {
          tx.rollback();
        } else {
          tx.setRollbackOnly();
        }
      }
      throw e;
    }
  }
}