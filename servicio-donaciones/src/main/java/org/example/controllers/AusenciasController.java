package org.example.controllers;

import io.javalin.http.Context;
import io.javalin.http.ServiceUnavailableResponse;
import io.javalin.http.UnauthorizedResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.function.Supplier;

import javax.persistence.EntityManager;

import org.example.dominio.notificacion.NotificacionesAusencia;

public final class AusenciasController {

  private final Supplier<EntityManager> proveedor;

  private final NotificacionesAusencia notificaciones =
      new NotificacionesAusencia();

  public AusenciasController(Supplier<EntityManager> proveedor) {
    this.proveedor = proveedor;
  }

  public void ejecutar(Context ctx) {
    ctx.header("Cache-Control", "no-store");

    String token = System.getenv("AUSENCIAS_CRON_TOKEN");

    if (token == null || token.length() < 32) {
      throw new ServiceUnavailableResponse(
          "El cron de ausencias no está configurado."
      );
    }

    String recibido = Objects.toString(
        ctx.header("Authorization"),
        ""
    );

    if (!MessageDigest.isEqual(
        ("Bearer " + token).getBytes(StandardCharsets.UTF_8),
        recibido.getBytes(StandardCharsets.UTF_8)
    )) {
      throw new UnauthorizedResponse(
          "Credencial de integración inválida."
      );
    }

    ctx.json(
        notificaciones.generarLote(proveedor.get())
    );
  }
}