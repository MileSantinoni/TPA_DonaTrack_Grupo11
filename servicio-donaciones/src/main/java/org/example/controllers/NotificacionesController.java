package org.example.controllers;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

import org.example.dominio.notificacion.NotificacionPendiente;
import org.example.seguridad.CuentaWeb;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

public class NotificacionesController {

  private static final int TAMANIO = 20;

  private static final DateTimeFormatter FECHA =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final Supplier<EntityManager> proveedor;

  public NotificacionesController(Supplier<EntityManager> proveedor) {
    this.proveedor = proveedor;
  }

  public void listar(Context ctx) {
    String cuentaId = cuentaActual(ctx);
    EntityManager em = proveedor.get();

    String filtro = Objects.toString(ctx.queryParam("leidas"), "false");

    if (!Set.of("true", "false").contains(filtro)) {
      throw new BadRequestResponse("Filtro inválido.");
    }

    boolean leidas = Boolean.parseBoolean(filtro);
    int pagina = pagina(ctx);

    String condicion = leidas
        ? "n.fechaLectura IS NOT NULL"
        : "n.fechaLectura IS NULL";

    List<NotificacionPendiente> encontradas = em.createQuery(
            "SELECT n FROM NotificacionPendiente n "
                + "WHERE n.cuentaId = :cuenta "
                + "AND n.fechaOcultada IS NULL "
                + "AND " + condicion + " "
                + "ORDER BY n.fechaCreacion DESC, n.id DESC",
            NotificacionPendiente.class
        )
        .setParameter("cuenta", cuentaId)
        .setFirstResult(pagina * TAMANIO)
        .setMaxResults(TAMANIO + 1)
        .getResultList();

    List<Map<String, Object>> items = new ArrayList<>();

    for (NotificacionPendiente n :
        encontradas.subList(0, Math.min(TAMANIO, encontradas.size()))) {

      items.add(Map.of(
          "id", n.getId(),
          "titulo", Objects.toString(
              n.getTitulo(),
              "Novedades de DonaTrack"
          ),
          "mensaje", n.getMensaje(),
          "fecha", n.getFechaCreacion().format(FECHA),
          "leida", n.estaLeida(),
          "rutaAccion", Objects.toString(n.getRutaAccion(), "")
      ));
    }

    ctx.json(Map.of(
        "items", items,
        "sinLeer", contar(em, cuentaId, false),
        "leidas", contar(em, cuentaId, true),
        "pagina", pagina,
        "hayMas", encontradas.size() > TAMANIO
    ));
  }

  public void leer(Context ctx) {
    modificarUna(ctx, false);
  }

  public void ocultar(Context ctx) {
    modificarUna(ctx, true);
  }

  public void leerTodas(Context ctx) {
    String cuentaId = cuentaActual(ctx);
    LogtoController.validarCsrf(ctx);

    EntityManager em = proveedor.get();
    var tx = em.getTransaction();

    try {
      tx.begin();

      em.createQuery(
              "UPDATE NotificacionPendiente n "
                  + "SET n.fechaLectura = :fecha, "
                  + "n.version = n.version + 1 "
                  + "WHERE n.cuentaId = :cuenta "
                  + "AND n.fechaOcultada IS NULL "
                  + "AND n.fechaLectura IS NULL"
          )
          .setParameter("fecha", LocalDateTime.now())
          .setParameter("cuenta", cuentaId)
          .executeUpdate();

      tx.commit();
      ctx.status(204);

    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
    }
  }

  private void modificarUna(Context ctx, boolean ocultar) {
    String cuentaId = cuentaActual(ctx);
    LogtoController.validarCsrf(ctx);

    EntityManager em = proveedor.get();
    var tx = em.getTransaction();

    try {
      tx.begin();

      // Se comprueba propiedad antes de modificar cualquier dato.
      List<NotificacionPendiente> resultados = em.createQuery(
              "SELECT n FROM NotificacionPendiente n "
                  + "WHERE n.id = :id AND n.cuentaId = :cuenta",
              NotificacionPendiente.class
          )
          .setParameter("id", ctx.pathParam("id"))
          .setParameter("cuenta", cuentaId)
          .setMaxResults(1)
          .getResultList();

      if (resultados.isEmpty()) {
        throw new NotFoundResponse("No existe esa notificación.");
      }

      String campo = ocultar ? "fechaOcultada" : "fechaLectura";

      em.createQuery(
              "UPDATE NotificacionPendiente n "
                  + "SET n." + campo + " = :fecha, "
                  + "n.version = n.version + 1 "
                  + "WHERE n.id = :id "
                  + "AND n.cuentaId = :cuenta "
                  + "AND n." + campo + " IS NULL"
          )
          .setParameter("fecha", LocalDateTime.now())
          .setParameter("id", ctx.pathParam("id"))
          .setParameter("cuenta", cuentaId)
          .executeUpdate();

      tx.commit();
      ctx.status(204);

    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
    }
  }

  private long contar(
      EntityManager em,
      String cuentaId,
      boolean leidas
  ) {
    String condicion = leidas
        ? "n.fechaLectura IS NOT NULL"
        : "n.fechaLectura IS NULL";

    return em.createQuery(
            "SELECT COUNT(n) FROM NotificacionPendiente n "
                + "WHERE n.cuentaId = :cuenta "
                + "AND n.fechaOcultada IS NULL "
                + "AND " + condicion,
            Long.class
        )
        .setParameter("cuenta", cuentaId)
        .getSingleResult();
  }

  private String cuentaActual(Context ctx) {
    ctx.header("Cache-Control", "no-store");
    return CuentaWeb.clave(LogtoController.usuario(ctx));
  }

  private int pagina(Context ctx) {
    try {
      int pagina = Integer.parseInt(
          Objects.toString(ctx.queryParam("pagina"), "0")
      );

      if (pagina < 0 || pagina > 100000) {
        throw new NumberFormatException();
      }

      return pagina;

    } catch (NumberFormatException e) {
      throw new BadRequestResponse("Página inválida.");
    }
  }
}