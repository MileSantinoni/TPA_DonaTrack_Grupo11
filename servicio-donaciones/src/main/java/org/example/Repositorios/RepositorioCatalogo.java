package org.example.Repositorios;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import org.example.dominio.catalogo.Bien;
import org.example.dominio.catalogo.Categoria;
import org.example.dominio.catalogo.Subcategoria;

public class RepositorioCatalogo {

  private final Supplier<EntityManager> proveedor;

  public RepositorioCatalogo(EntityManager em) {
    Objects.requireNonNull(em);
    this.proveedor = () -> em;
  }

  public RepositorioCatalogo(Supplier<EntityManager> proveedor) {
    this.proveedor = Objects.requireNonNull(proveedor);
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

  public void agregarCategoria(Categoria categoria) {
    EntityManager em = em();
    enTransaccion(em, () -> em.persist(categoria));
  }

  public void agregarBien(Bien bien) {
    EntityManager em = em();
    enTransaccion(em, () -> em.persist(bien));
  }

  public void actualizarCategoria(Categoria categoria) {
    EntityManager em = em();
    enTransaccion(em, () -> em.merge(categoria));
  }

  public void actualizarBien(Bien bien) {
    EntityManager em = em();
    enTransaccion(em, () -> em.merge(bien));
  }

  public Optional<Categoria> buscarCategoria(Long id) {
    return Optional.ofNullable(em().find(Categoria.class, id));
  }

  public Optional<Subcategoria> buscarSubcategoria(String id) {
    return Optional.ofNullable(em().find(Subcategoria.class, id));
  }

  public Optional<Bien> buscarBien(String id) {
    return Optional.ofNullable(em().find(Bien.class, id));
  }

  public List<Categoria> buscarCategorias() {
    return em().createQuery(
        "SELECT c FROM Categoria c ORDER BY c.nombre",
        Categoria.class
    ).getResultList();
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