package org.example.Repositorios;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import org.example.dominio.donacion.Donacion;

public class RepositorioDonaciones implements WithSimplePersistenceUnit {

  // Compatibilidad temporal con los llamados anteriores.
  private static final RepositorioDonaciones INSTANCE =
      new RepositorioDonaciones();

  private final Supplier<EntityManager> proveedor;

  private RepositorioDonaciones() {
    this.proveedor = this::entityManager;
  }

  public RepositorioDonaciones(EntityManager em) {
    Objects.requireNonNull(em);
    this.proveedor = () -> em;
  }

  public RepositorioDonaciones(Supplier<EntityManager> proveedor) {
    this.proveedor = Objects.requireNonNull(proveedor);
  }

  public static RepositorioDonaciones getInstance() {
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

  public void agregar(Donacion donacion) {
    EntityManager em = em();
    enTransaccion(em, () -> em.persist(donacion));
  }

  public void actualizar(Donacion donacion) {
    EntityManager em = em();
    enTransaccion(em, () -> em.merge(donacion));
  }

  public void eliminar(Donacion donacion) {
    String id = donacion.getIdAsString();

    if (id == null) {
      return;
    }

    EntityManager em = em();

    enTransaccion(em, () -> {
      Donacion encontrada = em.find(
          Donacion.class, UUID.fromString(id)
      );

      if (encontrada != null) {
        em.remove(encontrada);
      }
    });
  }

  public List<Donacion> buscarTodas() {
    return em().createQuery(
        "SELECT d FROM Donacion d", Donacion.class
    ).getResultList();
  }

  public Optional<Donacion> buscarPorId(String idTexto) {
    if (idTexto == null || idTexto.isBlank()) {
      return Optional.empty();
    }

    UUID id;

    try {
      id = UUID.fromString(idTexto);
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }

    return Optional.ofNullable(em().find(Donacion.class, id));
  }

  // Utilizar únicamente sobre la base de pruebas.
  public void limpiar() {
    EntityManager em = em();

    enTransaccion(em, () -> {
      List<Donacion> donaciones = em.createQuery(
          "SELECT d FROM Donacion d", Donacion.class
      ).getResultList();

      donaciones.forEach(em::remove);
    });
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