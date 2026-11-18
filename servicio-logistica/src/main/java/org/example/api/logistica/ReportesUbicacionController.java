package org.example.api.logistica;

import io.javalin.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import org.example.dominio.logistica.*;

public class ReportesUbicacionController {

  private final Supplier<EntityManager> proveedor;

  public record Reporte(
      String idRuta,
      String patente,
      Double latitud,
      Double longitud,
      Double velocidadKmh,
      String fechaYHora
  ) {}

  public ReportesUbicacionController(
      Supplier<EntityManager> proveedor
  ) {
    this.proveedor = proveedor;
  }

  private void autenticar(Context ctx) {
    String esperado = System.getenv("PLANIFICACION_TOKEN");
    String recibido = ctx.header("Authorization");

    if (esperado == null || esperado.isBlank()) {
      throw new ServiceUnavailableResponse(
          "Falta configurar la credencial de integración."
      );
    }

    if (recibido == null || !recibido.startsWith("Bearer ")) {
      throw new UnauthorizedResponse("Credencial inválida.");
    }

    if (!MessageDigest.isEqual(
        esperado.getBytes(StandardCharsets.UTF_8),
        recibido.substring(7).getBytes(StandardCharsets.UTF_8)
    )) {
      throw new UnauthorizedResponse("Credencial inválida.");
    }
  }

  private static String validarId(String valor) {
    try {
      return UUID.fromString(valor).toString();
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new BadRequestResponse("ID de ruta inválido.");
    }
  }

  private static void validarActiva(Ruta ruta) {
    if (ruta == null) {
      throw new NotFoundResponse("La ruta no existe.");
    }

    if (!ruta.estaActiva()
        || ruta.getEntregas().stream().noneMatch(
        e -> e.getEstado() == EstadoEntrega.EN_TRASLADO
    )) {
      throw new ConflictResponse(
          "La ruta debe estar iniciada y tener entregas en traslado."
      );
    }
  }

  private static boolean rango(Double n, double min, double max) {
    return n != null && Double.isFinite(n) && n >= min && n <= max;
  }

  public void recorrido(Context ctx) {
    autenticar(ctx);
    ctx.header("Cache-Control", "no-store");

    Ruta ruta = proveedor.get().find(
        Ruta.class,
        validarId(ctx.pathParam("idRuta"))
    );

    validarActiva(ruta);

    String geometria = ruta.getGeometriaPlanificada();

    if (geometria == null || geometria.isBlank()) {
      throw new ConflictResponse(
          "La ruta no tiene un recorrido guardado."
      );
    }

    ctx.json(Map.of(
        "idRuta", ruta.getId(),
        "patente", ruta.getCamion().getPatente(),
        "geometria", geometria
    ));
  }

  public void recibir(Context ctx) {
    autenticar(ctx);
    ctx.header("Cache-Control", "no-store");

    Reporte reporte = ctx.bodyAsClass(Reporte.class);

    if (reporte == null
        || reporte.patente() == null
        || reporte.patente().isBlank()) {
      throw new BadRequestResponse("La patente es obligatoria.");
    }

    String idRuta = validarId(reporte.idRuta());

    if (!rango(reporte.latitud(), -90, 90)
        || !rango(reporte.longitud(), -180, 180)
        || !rango(reporte.velocidadKmh(), 0, 200)) {
      throw new BadRequestResponse(
          "Coordenadas inválidas o velocidad fuera de 0 a 200 km/h."
      );
    }

    Instant fecha;

    try {
      fecha = OffsetDateTime.parse(
          reporte.fechaYHora()
      ).toInstant();
    } catch (DateTimeException | NullPointerException e) {
      throw new BadRequestResponse(
          "fechaYHora debe incluir zona horaria."
      );
    }

    Instant ahora = Instant.now();

    // Contrato de reportes en vivo: no admite cargas históricas.
    if (fecha.isBefore(ahora.minusSeconds(120))
        || fecha.isAfter(ahora.plusSeconds(30))) {
      throw new BadRequestResponse(
          "El reporte debe ser reciente. Revisá el reloj del emisor."
      );
    }

    var em = proveedor.get();
    var tx = em.getTransaction();

    try {
      tx.begin();

      Ruta ruta = em.find(
          Ruta.class, idRuta, LockModeType.PESSIMISTIC_WRITE
      );

      validarActiva(ruta);

      if (!ruta.getCamion().getPatente().equals(reporte.patente())) {
        throw new ConflictResponse(
            "La patente no corresponde a esta ruta."
        );
      }

      Camion camion = em.find(
          Camion.class,
          reporte.patente(),
          LockModeType.PESSIMISTIC_WRITE
      );

      long activas = em.createQuery("""
                SELECT COUNT(DISTINCT r)
                FROM Ruta r JOIN r.entregas e
                WHERE r.camion.patente = :patente
                  AND r.activa = true
                  AND e.estado = :estado
                """, Long.class)
          .setParameter("patente", reporte.patente())
          .setParameter("estado", EstadoEntrega.EN_TRASLADO)
          .getSingleResult();

      if (activas != 1) {
        throw new ConflictResponse(
            "El camión tiene más de una ruta en traslado."
        );
      }

      var ultima = camion.getUltimaUbicacion();

      if (ultima != null && ultima.getFechaReporteUtc() != null) {
        int orden = fecha.compareTo(ultima.getFechaReporteUtc());

        if (orden < 0) {
          tx.commit();
          ctx.json(Map.of(
              "actualizado", false,
              "motivo", "REPORTE_ANTIGUO"
          ));
          return;
        }

        if (orden == 0) {
          boolean mismo =
              idRuta.equals(ultima.getRutaReporteId())
                  && Double.compare(
                  ultima.getLatitud(), reporte.latitud()
              ) == 0
                  && Double.compare(
                  ultima.getLongitud(), reporte.longitud()
              ) == 0
                  && Double.compare(
                  ultima.getVelocidad(), reporte.velocidadKmh()
              ) == 0;

          if (!mismo) {
            throw new ConflictResponse(
                "Ya existe otro reporte con esa fecha."
            );
          }

          tx.commit();
          ctx.json(Map.of(
              "actualizado", false,
              "motivo", "REPORTE_DUPLICADO"
          ));
          return;
        }
      }

      camion.actualizarUbicacion(new UbicacionCamion(
          reporte.latitud(),
          reporte.longitud(),
          reporte.velocidadKmh(),
          LocalDateTime.ofInstant(
              fecha,
              ZoneId.of("America/Argentina/Buenos_Aires")
          )
      ));

      camion.getUltimaUbicacion().identificarReporte(idRuta, fecha);

      for (var entrega : ruta.getEntregas()) {
        entrega.registrarLlegadaSiCorresponde(
            camion.getUltimaUbicacion().getLatitud(),
            camion.getUltimaUbicacion().getLongitud(),
            fecha
        );
      }

      tx.commit();

      ctx.json(Map.of(
          "actualizado", true,
          "fechaReporte", fecha.toString()
      ));

    } catch (RuntimeException e) {
      if (tx.isActive()) tx.rollback();
      throw e;
    }
  }
}