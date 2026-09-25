package org.example.Repositorios;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import org.example.dominio.donante.Donante;

public class RepositorioDonantes implements WithSimplePersistenceUnit {

  // Compatibilidad temporal con llamados anteriores.
  private static final RepositorioDonantes INSTANCE =
      new RepositorioDonantes();

  private final Supplier<EntityManager> proveedor;

  private RepositorioDonantes() {
    this.proveedor = this::entityManager;
  }

  public RepositorioDonantes(EntityManager em) {
    Objects.requireNonNull(em);
    this.proveedor = () -> em;
  }

  public RepositorioDonantes(Supplier<EntityManager> proveedor) {
    this.proveedor = Objects.requireNonNull(proveedor);
  }

  public static RepositorioDonantes getInstance() {
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

  public void agregar(Donante donante) {
    EntityManager em = em();
    enTransaccion(em, () -> em.persist(donante));
  }

  public void actualizar(Donante donante) {
    EntityManager em = em();
    enTransaccion(em, () -> em.merge(donante));
  }

  public void eliminar(Donante donante) {
    if (donante.getId() == null) {
      return;
    }

    EntityManager em = em();

    enTransaccion(em, () -> {
      Donante encontrado = em.find(Donante.class, donante.getId());

      if (encontrado != null) {
        em.remove(encontrado);
      }
    });
  }

  public List<Donante> buscarTodos() {
    return em().createQuery(
        "SELECT d FROM Donante d", Donante.class
    ).getResultList();
  }

  public Optional<Donante> buscarPorId(String idTexto) {
    if (idTexto == null || idTexto.isBlank()) {
      return Optional.empty();
    }

    Long id;

    try {
      id = Long.valueOf(idTexto);
    } catch (NumberFormatException e) {
      return Optional.empty();
    }

    return Optional.ofNullable(em().find(Donante.class, id));
  }

  public Optional<Donante> buscarPorMail(String mail) {
    return em().createQuery(
            "SELECT d FROM Donante d WHERE d.mail = :mail",
            Donante.class
        )
        .setParameter("mail", mail)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
  }

  public boolean existeMail(String mail) {
    return buscarPorMail(mail).isPresent();
  }

  // Utilizar únicamente sobre la base de pruebas.
  public void limpiar() {
    EntityManager em = em();

    enTransaccion(em, () -> {
      List<Donante> donantes = em.createQuery(
          "SELECT d FROM Donante d", Donante.class
      ).getResultList();

      donantes.forEach(em::remove);
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

  public void sincronizarYLiberarContexto() {
    EntityManager em = em();

    if (!em.getTransaction().isActive()) {
      throw new IllegalStateException(
          "La importacion debe ejecutarse dentro de una transaccion"
      );
    }

    em.flush();
    em.clear();
  }
}