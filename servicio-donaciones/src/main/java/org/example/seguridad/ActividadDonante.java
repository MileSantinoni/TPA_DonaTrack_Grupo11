package org.example.seguridad;

import io.javalin.http.Context;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Set;

import javax.persistence.EntityManager;

import org.slf4j.LoggerFactory;

public final class ActividadDonante {

  private ActividadDonante() {}

  public static void registrar(Context ctx, EntityManager em) {
    if (ctx.res().getStatus() < 200
        || ctx.res().getStatus() >= 400
        || em.getTransaction().isActive()) {
      return;
    }

    String ruta = ctx.path();

    if (ruta.startsWith("/assets/")
        || ruta.startsWith("/interno/")
        || ruta.equals("/graphql")
        || ruta.equals("/health")) {
      return;
    }

    String metodo = ctx.method().name();

    if (metodo.equals("GET")) {
      String accept = Objects.toString(
          ctx.header("Accept"),
          ""
      );

      String modo = ctx.header("Sec-Fetch-Mode");

      // Solo navegación de páginas, no consultas automáticas.
      if (!accept.contains("text/html")
          || (modo != null && !modo.equals("navigate"))
          || ruta.equals("/cuenta/notificaciones")) {
        return;
      }

    } else if (!Set.of("POST", "PUT", "PATCH", "DELETE")
        .contains(metodo)) {
      return;
    }

    var usuario = SesionWeb.opcional(ctx);

    if (usuario == null || SesionWeb.esAdmin(usuario)) {
      return;
    }

    var tx = em.getTransaction();

    try {
      tx.begin();

      em.createQuery(
              "UPDATE CuentaWeb c "
                  + "SET c.ultimaInteraccionUtc = :ahora, "
                  + "c.avisoAusenciaGenerado = false "
                  + "WHERE c.id = :id "
                  + "AND c.rol = 'DONANTE' "
                  + "AND c.donante IS NOT NULL "
                  + "AND c.ultimaInteraccionUtc < :ahora"
          )
          .setParameter(
              "ahora",
              LocalDateTime.now(ZoneOffset.UTC)
          )
          .setParameter("id", CuentaWeb.clave(usuario))
          .executeUpdate();

      tx.commit();

    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      LoggerFactory.getLogger(ActividadDonante.class)
          .warn(
              "No se pudo registrar la actividad del donante, tipo={}",
              e.getClass().getSimpleName()
          );
    }
  }
}