package org.example.dominio.notificacion;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.example.seguridad.CuentaWeb;

public final class NotificacionesAusencia {

  private static final int LOTE = 200;

  private final Clock reloj;

  public NotificacionesAusencia() {
    this(Clock.systemUTC());
  }

  public NotificacionesAusencia(Clock reloj) {
    this.reloj = Objects.requireNonNull(reloj);
  }

  public record Resultado(
      int evaluadas,
      int generadas,
      boolean continuar
  ) {}

  public Resultado generarLote(EntityManager em) {
    var tx = em.getTransaction();

    if (tx.isActive()) {
      throw new IllegalStateException(
          "La revisión requiere su propia transacción"
      );
    }

    LocalDateTime limite = LocalDateTime.ofInstant(
        reloj.instant(),
        ZoneOffset.UTC
    ).minusDays(20);

    try {
      tx.begin();

      var candidatas = em.createQuery(
              "SELECT c FROM CuentaWeb c "
                  + "WHERE c.rol = 'DONANTE' "
                  + "AND c.donante IS NOT NULL "
                  + "AND c.ultimaInteraccionUtc < :limite "
                  + "AND c.avisoAusenciaGenerado = false "
                  + "ORDER BY c.id",
              CuentaWeb.class
          )
          .setParameter("limite", limite)
          .setMaxResults(LOTE)
          .getResultList();

      int generadas = 0;

      for (CuentaWeb cuenta : candidatas) {
        // Puede haber actividad o una ejecución concurrente
        // desde que se consultaron las candidatas.
        em.refresh(cuenta, LockModeType.PESSIMISTIC_WRITE);

        if (!"DONANTE".equals(cuenta.rol)
            || cuenta.donante == null
            || cuenta.avisoAusenciaGenerado
            || !cuenta.ultimaInteraccionUtc.isBefore(limite)) {
          continue;
        }

        String operacion = "AUSENCIA:"
            + UUID.nameUUIDFromBytes(
            (cuenta.id + "|" + cuenta.ultimaInteraccionUtc)
                .getBytes(StandardCharsets.UTF_8)
        );

        var aviso = NotificacionesCuenta.preparar(
            em,
            cuenta.id,
            "DONANTE:" + cuenta.donante.getId(),
            operacion,
            "Te extrañamos en DonaTrack",
            "Pasaron más de 20 días desde tu última actividad. "
                + "Podés volver a consultar tus donaciones "
                + "y las necesidades de las entidades. "
                + "Si está a tu alcance, una nueva donación puede ayudar."
        );

        aviso.vincularAccion("/mis-donaciones");

        em.persist(aviso);
        cuenta.avisoAusenciaGenerado = true;
        generadas++;
      }

      tx.commit();

      return new Resultado(
          candidatas.size(),
          generadas,
          candidatas.size() == LOTE
      );

    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      em.clear();
      throw e;
    }
  }
}