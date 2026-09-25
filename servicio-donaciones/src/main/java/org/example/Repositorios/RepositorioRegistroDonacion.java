package org.example.Repositorios;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import org.example.dominio.donacion.RegistroDonacion;
import org.example.dominio.donacion.Donacion;

public class RepositorioRegistroDonacion
    implements WithSimplePersistenceUnit {

  // Compatibilidad temporal mientras migramos los consumidores.
  private static final RepositorioRegistroDonacion INSTANCE =
      new RepositorioRegistroDonacion();

  private final Supplier<EntityManager> proveedor;

  private RepositorioRegistroDonacion() {
    this.proveedor = this::entityManager;
  }

  public RepositorioRegistroDonacion(EntityManager em) {
    Objects.requireNonNull(em);
    this.proveedor = () -> em;
  }

  public RepositorioRegistroDonacion(
      Supplier<EntityManager> proveedor
  ) {
    this.proveedor = Objects.requireNonNull(proveedor);
  }

  public static RepositorioRegistroDonacion getInstance() {
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

  public void agregarRegistro(RegistroDonacion registro) {
    Objects.requireNonNull(registro);

    if (registro.getId() == null || registro.getId().isBlank()) {
      throw new IllegalArgumentException(
          "El registro debe tener un ID"
      );
    }

    EntityManager em = em();
    enTransaccion(em, () -> em.persist(registro));
  }

  public List<RegistroDonacion> obtenerTodos() {
    return em().createQuery(
        "SELECT r FROM RegistroDonacion r",
        RegistroDonacion.class
    ).getResultList();
  }

  public RegistroDonacion buscarPorId(String id) {
    if (id == null || id.isBlank()) {
      return null;
    }

    return em().find(RegistroDonacion.class, id);
  }

  // Utilizar únicamente sobre la base de pruebas.
  public void limpiarRepositorio() {
    EntityManager em = em();

    enTransaccion(em, () -> {
      List<RegistroDonacion> registros = em.createQuery(
          "SELECT r FROM RegistroDonacion r",
          RegistroDonacion.class
      ).getResultList();

      registros.forEach(em::remove);
    });
  }

  private void enTransaccion(
      EntityManager em,
      Runnable operacion
  ) {
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

  public void agregarRegistroConDonaciones(
      RegistroDonacion registro,
      List<Donacion> donaciones
  ) {
    Objects.requireNonNull(registro);
    Objects.requireNonNull(donaciones);

    if (registro.getId() == null || registro.getId().isBlank()) {
      throw new IllegalArgumentException("El registro debe tener un ID");
    }

    if (donaciones.isEmpty()) {
      throw new IllegalArgumentException(
          "El registro debe generar al menos una donacion"
      );
    }

    EntityManager em = em();

    enTransaccion(em, () -> {
      em.persist(registro);
      donaciones.forEach(em::persist);
    });
  }
}