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
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.EventoLogisticoProcesado;
import javax.persistence.LockModeType;
import org.example.dominio.notificacion.NotificacionPendiente;
import org.example.dominio.notificacion.Notificacion;
import org.example.dominio.notificacion.EstadoNotificacion;
import org.example.dominio.notificacion.Notificador;

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
    enTransaccion(em, () -> {
      resolverSubcategorias(em, asignacion);
      em.persist(asignacion);
    });
  }

  private void resolverSubcategorias(EntityManager em, AsignacionDonacion asignacion) {
    if (asignacion.getDonacion() != null && asignacion.getDonacion().getSubcategoria() != null) {
      asignacion.getDonacion().setSubcategoria(
          resolverSubcategoria(em, asignacion.getDonacion().getSubcategoria())
      );
    }
    if (asignacion.getNecesidad() != null && asignacion.getNecesidad().getSubcategoria() != null) {
      asignacion.getNecesidad().setSubcategoria(
          resolverSubcategoria(em, asignacion.getNecesidad().getSubcategoria())
      );
    }
    if (asignacion.getEntidad() != null) {
      for (org.example.dominio.beneficiario.Necesidad nec : asignacion.getEntidad().getNecesidades()) {
        if (nec.getSubcategoria() != null) {
          nec.setSubcategoria(resolverSubcategoria(em, nec.getSubcategoria()));
        }
      }
    }
  }

  private Subcategoria resolverSubcategoria(EntityManager em, Subcategoria sub) {
    if (sub == null) return null;
    Subcategoria existente = em.find(Subcategoria.class, sub.getId());
    if (existente == null) {
      em.persist(sub);
      return sub;
    }
    return existente;
  }

  public void actualizar(AsignacionDonacion asignacion) {
    Objects.requireNonNull(asignacion);

    EntityManager em = em();
    enTransaccion(em, () -> em.merge(asignacion));
  }

  public void eliminar(AsignacionDonacion asignacion) {
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

  public void limpiarCache() {
    em().clear();
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
    em.clear();
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

  public <T> T ejecutarEnTransaccion(Supplier<T> operacion) {
    Objects.requireNonNull(operacion);

    EntityManager em = em();
    EntityTransaction tx = em.getTransaction();

    if (tx.isActive()) {
      throw new IllegalStateException(
          "Esta operacion requiere administrar su propia transaccion"
      );
    }

    try {
      tx.begin();
      T resultado = operacion.get();
      tx.commit();
      return resultado;
    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }
      throw e;
    }
  }

  public Optional<EventoLogisticoProcesado> buscarEventoProcesado(
      String idOperacion
  ) {
    return Optional.ofNullable(
        em().find(EventoLogisticoProcesado.class, idOperacion)
    );
  }

  public void guardarEventoProcesado(EventoLogisticoProcesado evento) {
    EntityManager actual = em();

    if (!actual.getTransaction().isActive()) {
      throw new IllegalStateException(
          "El evento debe guardarse dentro de la transaccion del lote"
      );
    }

    actual.persist(evento);
  }

  public void guardarNotificacionesPendientes(
      List<NotificacionPendiente> pendientes
  ) {
    EntityManager actual = em();

    if (!actual.getTransaction().isActive()) {
      throw new IllegalStateException(
          "Las notificaciones deben guardarse en la transaccion del evento"
      );
    }

    pendientes.forEach(actual::persist);
  }

  public List<String> buscarIdsNotificacionesPendientes(int limite) {
    if (limite <= 0) {
      throw new IllegalArgumentException("El limite debe ser positivo");
    }

    return em().createQuery(
            "SELECT n.id FROM NotificacionPendiente n "
                + "WHERE n.estado <> :completada "
                + "ORDER BY n.ultimoIntento ASC, n.fechaCreacion ASC, n.id ASC",
            String.class
        )
        .setParameter("completada", EstadoNotificacion.COMPLETADA)
        .setMaxResults(limite)
        .getResultList();
  }

  public List<NotificacionPendiente> buscarNotificacionesDeEvento(
      String idOperacion
  ) {
    return em().createQuery(
            "SELECT n FROM NotificacionPendiente n "
                + "WHERE n.idOperacion = :idOperacion",
            NotificacionPendiente.class
        )
        .setParameter("idOperacion", idOperacion)
        .getResultList();
  }

  public boolean procesarNotificacionPendiente(
      String id,
      Notificador notificador
  ) {
    return ejecutarEnTransaccion(() -> {
      EntityManager actual = em();

      NotificacionPendiente pendiente =
          actual.find(NotificacionPendiente.class, id);

      if (pendiente == null) {
        return false;
      }

      // Recarga y bloquea el registro antes de decidir si hay que enviarlo.
      actual.refresh(pendiente, LockModeType.PESSIMISTIC_WRITE);

      if (!pendiente.requiereEnvio()) {
        return false;
      }

      Notificacion resultado;

      try {
        resultado = notificador.enviarPendiente(pendiente);
      } catch (RuntimeException errorDeEnvio) {
        pendiente.registrarFallo();
        return true;
      }

      pendiente.registrarResultado(resultado);
      return true;
    });
  }

  public void bloquearDonacion(
      org.example.dominio.donacion.Donacion donacion
  ) {
    em().refresh(donacion, LockModeType.PESSIMISTIC_WRITE);
  }

  public void bloquearNecesidades(
      java.util.Collection<
          org.example.dominio.beneficiario.Necesidad
          > necesidades
  ) {
    necesidades.stream()
        .distinct()
        .sorted(java.util.Comparator.comparing(n -> n.getId()))
        .forEach(n ->
            em().refresh(n, LockModeType.PESSIMISTIC_WRITE)
        );
  }

  public void bloquearParaEvento(
      List<AsignacionDonacion> seleccionadas
  ) {
    seleccionadas.stream()
        .map(AsignacionDonacion::getDonacion)
        .distinct()
        .sorted(java.util.Comparator.comparing(
            d -> d.getIdAsString()
        ))
        .forEach(this::bloquearDonacion);

    bloquearNecesidades(
        seleccionadas.stream()
            .map(AsignacionDonacion::getNecesidad)
            .filter(Objects::nonNull)
            .toList()
    );
  }
}