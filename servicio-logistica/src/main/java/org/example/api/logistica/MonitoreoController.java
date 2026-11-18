package org.example.api.logistica;

import io.javalin.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.example.dominio.logistica.*;

public class MonitoreoController {

  private final Supplier<EntityManager> proveedor;

  public MonitoreoController(Supplier<EntityManager> proveedor) {
    this.proveedor = proveedor;
  }

  public void consultar(Context ctx) {
    String token = System.getenv("PLANIFICACION_TOKEN");

    if (token == null || token.isBlank()) {
      throw new ServiceUnavailableResponse(
          "Falta configurar PLANIFICACION_TOKEN."
      );
    }

    String recibido = Objects.toString(
        ctx.header("X-Planificacion-Token"), ""
    );

    if (!MessageDigest.isEqual(
        token.getBytes(StandardCharsets.UTF_8),
        recibido.getBytes(StandardCharsets.UTF_8)
    )) {
      throw new UnauthorizedResponse("Credencial inválida.");
    }

    EntityManager em = proveedor.get();
    Instant ahora = Instant.now();

    var camiones = em.createQuery(
        "SELECT c FROM Camion c ORDER BY c.patente",
        Camion.class
    ).getResultList();

    var rutas = em.createQuery(
        "SELECT DISTINCT r FROM Ruta r LEFT JOIN FETCH r.entregas",
        Ruta.class
    ).getResultList();

    Map<String, List<Ruta>> porCamion = new HashMap<>();

    for (Ruta ruta : rutas) {
      porCamion.computeIfAbsent(
          ruta.getCamion().getPatente(),
          ignorado -> new ArrayList<>()
      ).add(ruta);
    }

    List<Map<String, Object>> filas = new ArrayList<>();

    for (Camion camion : camiones) {
      var propias = porCamion.getOrDefault(
          camion.getPatente(), List.of()
      );

      var pendientes = propias.stream()
          .filter(r -> r.getEntregas().stream()
              .anyMatch(e -> !e.fueResuelta()))
          .toList();

      var iniciadas = pendientes.stream()
          .filter(Ruta::estaActiva)
          .toList();

      Ruta elegida = null;
      boolean conflicto = iniciadas.size() > 1;

      if (iniciadas.size() == 1) {
        elegida = iniciadas.get(0);
      } else if (!conflicto && pendientes.size() == 1) {
        elegida = pendientes.get(0);
      }

      var ubicacion = camion.getUltimaUbicacion();

      // Cuando las entregas ya están resueltas, conservamos
      // la última ruta reportada como referencia administrativa.
      if (elegida == null && pendientes.isEmpty()
          && ubicacion != null
          && ubicacion.getRutaReporteId() != null) {

        elegida = propias.stream()
            .filter(r -> r.getId().equals(
                ubicacion.getRutaReporteId()
            ))
            .findFirst()
            .orElse(null);
      }

      Map<String, Object> fila = new HashMap<>();
      fila.put("patente", camion.getPatente());

      String estado;

      if (conflicto) {
        estado = "Varias rutas iniciadas";
      } else if (elegida != null && pendientes.isEmpty()) {
        estado = "Entregas resueltas";
      } else if (!iniciadas.isEmpty()) {
        estado = "En ruta";
      } else if (!pendientes.isEmpty()) {
        estado = "Con ruta asignada";
      } else {
        estado = camion.estaDisponible()
            ? "Disponible" : "No disponible";
      }

      fila.put("estado", estado);
      fila.put("rutasPendientes", pendientes.size());
      fila.put("total", 0);
      fila.put("completadas", 0);
      fila.put("noRecibidas", 0);
      fila.put("entregas", List.of());
      fila.put("reciente", false);

      if (elegida != null) {
        fila.put("idRuta", elegida.getId());
        fila.put("fecha", Objects.toString(
            elegida.getFechaPlanificada(), ""
        ));
        fila.put("geometria", Objects.toString(
            elegida.getGeometriaPlanificada(), ""
        ));

        var entregas = elegida.getEntregas();

        fila.put("total", entregas.size());
        fila.put("completadas", entregas.stream()
            .filter(e -> e.getEstado() == EstadoEntrega.ENTREGADA)
            .count());
        fila.put("noRecibidas", entregas.stream()
            .filter(e -> e.getEstado() == EstadoEntrega.NO_RECIBIDA)
            .count());

        fila.put("entregas", entregas.stream()
            .sorted(Comparator.comparingInt(Entrega::getOrden))
            .map(e -> Map.of(
                "orden", e.getOrden(),
                "entidad", Objects.toString(e.getRazonSocial(), ""),
                "direccion", Objects.toString(e.getDireccion(), ""),
                "estado", e.getEstado().name()
            ))
            .toList());

        var deposito = elegida.getUbicacionDeposito();

        if (deposito != null) {
          fila.put("deposito", Map.of(
              "lat", deposito.getLatitud(),
              "lng", deposito.getLongitud()
          ));
        }

        if (ubicacion != null
            && ubicacion.getFechaReporteUtc() != null
            && elegida.getId().equals(ubicacion.getRutaReporteId())) {

          long antiguedad = Math.max(0, Duration.between(
              ubicacion.getFechaReporteUtc(), ahora
          ).getSeconds());

          fila.put("reciente", antiguedad <= 60);
          fila.put("ubicacion", Map.of(
              "lat", ubicacion.getLatitud(),
              "lng", ubicacion.getLongitud(),
              "velocidad", ubicacion.getVelocidad(),
              "fecha", ubicacion.getFechaReporteUtc().toString()
          ));
        }
      }

      filas.add(fila);
    }

    ctx.header("Cache-Control", "no-store")
        .json(Map.of(
            "camiones", filas,
            "consultadoEn", ahora.toString()
        ));
  }
}