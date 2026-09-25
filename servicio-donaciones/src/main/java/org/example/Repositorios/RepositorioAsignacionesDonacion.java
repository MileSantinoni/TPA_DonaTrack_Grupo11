package org.example.Repositorios;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import org.example.dominio.donacion.AsignacionDonacion;

public class RepositorioAsignacionesDonacion
    implements WithSimplePersistenceUnit {

  // Compatibilidad temporal mientras migramos los consumidores.
  private static final RepositorioAsignacionesDonacion INSTANCE =
      new RepositorioAsignacionesDonacion();

  private final Supplier<EntityManager> proveedor;

  private RepositorioAsignacionesDonacion() {
    this.proveedor = this::entityManager;
  }

  public RepositorioAsignacionesDonacion(EntityManager em) {
    Objects.requireNonNull(em);
    this.proveedor = () -> em;
  }

  public RepositorioAsignacionesDonacion(
      Supplier<EntityManager> proveedor
  ) {
    this.proveedor = Objects.requireNonNull(proveedor);
  }

  public static RepositorioAsignacionesDonacion getInstance() {
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

  public void agregar(AsignacionDonacion asignacion) {
    Objects.requireNonNull(asignacion);

    EntityManager em = em();
    enTransaccion(em, () -> em.persist(asignacion));
  }

  public void actualizar(AsignacionDonacion asignacion) {
    Objects.requireNonNull(asignacion);

    EntityManager em = em();
    enTransaccion(em, () -> em.merge(asignacion));
  }

  public void eliminar(AsignacionDonacion asignacion) {
    Objects.requireNonNull(asignacion);

    String idTexto = asignacion.getIdAsString();

    if (idTexto == null) {
      return;
    }

    UUID id = UUID.fromString(idTexto);
    EntityManager em = em();

    enTransaccion(em, () -> {
      AsignacionDonacion encontrada =
          em.find(AsignacionDonacion.class, id);

      if (encontrada != null) {
        em.remove(encontrada);
      }
    });
  }

  public List<AsignacionDonacion> buscarTodas() {
    return em().createQuery(
        "SELECT a FROM AsignacionDonacion a",
        AsignacionDonacion.class
    ).getResultList();
  }

  public Optional<AsignacionDonacion> buscarPorId(String idTexto) {
    UUID id = interpretarId(idTexto);

    if (id == null) {
      return Optional.empty();
    }

    return Optional.ofNullable(
        em().find(AsignacionDonacion.class, id)
    );
  }

  public List<AsignacionDonacion> buscarPorEntidad(String idEntidad) {
    UUID id = interpretarId(idEntidad);

    if (id == null) {
      return List.of();
    }

    return em().createQuery(
            "SELECT a FROM AsignacionDonacion a "
                + "WHERE a.entidad.id = :idEntidad",
            AsignacionDonacion.class
        )
        .setParameter("idEntidad", id)
        .getResultList();
  }

  public List<AsignacionDonacion> buscarEntreFechas(
      LocalDate desde,
      LocalDate hasta
  ) {
    if (desde == null || hasta == null || desde.isAfter(hasta)) {
      throw new IllegalArgumentException(
          "El rango de fechas es invalido"
      );
    }

    return em().createQuery(
            "SELECT a FROM AsignacionDonacion a "
                + "WHERE a.fechaRecepcion BETWEEN :desde AND :hasta "
                + "ORDER BY a.fechaRecepcion ASC",
            AsignacionDonacion.class
        )
        .setParameter("desde", desde)
        .setParameter("hasta", hasta)
        .getResultList();
  }

  // Utilizar únicamente sobre la base de pruebas.
  public void limpiar() {
    EntityManager em = em();

    enTransaccion(em, () -> {
      List<AsignacionDonacion> asignaciones = em.createQuery(
          "SELECT a FROM AsignacionDonacion a",
          AsignacionDonacion.class
      ).getResultList();

      asignaciones.forEach(em::remove);
    });
  }

  private UUID interpretarId(String texto) {
    if (texto == null || texto.isBlank()) {
      return null;
    }

    try {
      return UUID.fromString(texto);
    } catch (IllegalArgumentException e) {
      return null;
    }
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