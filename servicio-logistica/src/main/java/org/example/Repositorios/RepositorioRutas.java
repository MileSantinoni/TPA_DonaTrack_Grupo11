package org.example.Repositorios;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import org.example.dominio.logistica.Ruta;

public class RepositorioRutas {

  private final Supplier<EntityManager> proveedorEntityManager;

  public RepositorioRutas(EntityManager entityManager) {
    Objects.requireNonNull(entityManager);
    this.proveedorEntityManager = () -> entityManager;
  }

  public RepositorioRutas(Supplier<EntityManager> proveedorEntityManager) {
    this.proveedorEntityManager =
        Objects.requireNonNull(proveedorEntityManager);
  }

  private EntityManager em() {
    EntityManager entityManager = proveedorEntityManager.get();

    if (entityManager == null || !entityManager.isOpen()) {
      throw new IllegalStateException(
          "No hay un EntityManager abierto para esta operacion"
      );
    }

    return entityManager;
  }

  public void agregar(Ruta ruta) {
    EntityManager entityManager = em();
    enTransaccion(entityManager, () -> entityManager.persist(ruta));
  }

  public void actualizar(Ruta ruta) {
    EntityManager entityManager = em();
    enTransaccion(entityManager, () -> entityManager.merge(ruta));
  }

  public Optional<Ruta> buscarPorId(String id) {
    return Optional.ofNullable(em().find(Ruta.class, id));
  }

  public List<Ruta> buscarTodos() {
    return em()
        .createQuery("SELECT r FROM Ruta r", Ruta.class)
        .getResultList();
  }

  public List<Ruta> buscarPorPatente(String patente) {
    return em()
        .createQuery(
            "SELECT r FROM Ruta r WHERE r.camion.patente = :patente",
            Ruta.class
        )
        .setParameter("patente", patente)
        .getResultList();
  }

  private void enTransaccion(
      EntityManager entityManager,
      Runnable operacion
  ) {
    EntityTransaction transaccion = entityManager.getTransaction();
    boolean propia = !transaccion.isActive();

    try {
      if (propia) {
        transaccion.begin();
      }

      operacion.run();

      if (propia) {
        transaccion.commit();
      }
    } catch (RuntimeException e) {
      if (transaccion.isActive()) {
        if (propia) {
          transaccion.rollback();
        } else {
          transaccion.setRollbackOnly();
        }
      }
      throw e;
    }
  }

  public void enTransaccion(Runnable operacion) {
    Objects.requireNonNull(operacion);
    enTransaccion(em(), operacion);
  }
}