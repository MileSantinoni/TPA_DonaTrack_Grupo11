package org.example.Repositorios;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.example.dominio.logistica.Camion;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class RepositorioCamiones implements WithSimplePersistenceUnit {

  private static final RepositorioCamiones instancia =
      new RepositorioCamiones();

  public static RepositorioCamiones getInstance() {
    return instancia;
  }

  private final Supplier<EntityManager> proveedorEntityManager;

  private RepositorioCamiones() {
    this.proveedorEntityManager = this::entityManager;
  }

  // Para pruebas o programas como PruebaConexion.
  public RepositorioCamiones(EntityManager entityManager) {
    Objects.requireNonNull(entityManager);
    this.proveedorEntityManager = () -> entityManager;
  }

  // Para obtener la conexion correspondiente a cada peticion.
  public RepositorioCamiones(Supplier<EntityManager> proveedor) {
    this.proveedorEntityManager = Objects.requireNonNull(proveedor);
  }

  private EntityManager em() {
    EntityManager actual = proveedorEntityManager.get();

    if (actual == null || !actual.isOpen()) {
      throw new IllegalStateException(
          "No hay un EntityManager abierto para esta operacion"
      );
    }

    return actual;
  }

  public void agregar(Camion camion) {
    EntityManager em = em();
    enTransaccion(em, () -> em.persist(camion));
  }

  public void actualizar(Camion camion) {
    EntityManager em = em();
    enTransaccion(em, () -> em.merge(camion));
  }

  public void eliminar(Camion camion) {
    EntityManager em = em();

    enTransaccion(em, () -> {
      Camion registrado = em.find(
          Camion.class, camion.getPatente()
      );

      if (registrado != null) {
        em.remove(registrado);
      }
    });
  }

  public List<Camion> buscarTodos() {
    return em()
        .createQuery("SELECT c FROM Camion c", Camion.class)
        .getResultList();
  }

  public List<Camion> buscarDisponibles() {
    return em()
        .createQuery(
            "SELECT c FROM Camion c WHERE c.disponible = true",
            Camion.class
        )
        .getResultList();
  }

  public Optional<Camion> buscarPorPatente(String patente) {
    return Optional.ofNullable(
        em().find(Camion.class, patente)
    );
  }

  // Borra todos los camiones: utilizar solo para limpiar pruebas.
  public void limpiar() {
    EntityManager em = em();

    enTransaccion(em, () -> {
      List<Camion> registrados = em.createQuery(
          "SELECT c FROM Camion c", Camion.class
      ).getResultList();

      registrados.forEach(em::remove);
    });
  }

  private void enTransaccion(EntityManager em, Runnable operacion) {
    EntityTransaction transaccion = em.getTransaction();
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
}