package org.example.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.example.dominio.donacion.*;

public class AdminPlanificacionController {

  private final Supplier<EntityManager> proveedor;

  private final ObjectMapper json = new ObjectMapper();

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  private final String logistica = System.getenv()
      .getOrDefault("LOGISTICA_URL", "http://localhost:8082")
      .replaceAll("/+$", "");

  public record Carga(
      Double pesoKg,
      Double volumenM3
  ) {}

  public record Pedido(
      Map<String, Carga> cargas
  ) {}

  public AdminPlanificacionController(
      Supplier<EntityManager> proveedor
  ) {
    this.proveedor = proveedor;
  }

  private List<AsignacionDonacion> asignadas() {
    return proveedor.get().createQuery("""
        SELECT a FROM AsignacionDonacion a
        JOIN FETCH a.donacion d
        JOIN FETCH a.entidad
        WHERE d.estadoActualTexto = :estado
        ORDER BY d.fechaDeRegistro, d.id
        """, AsignacionDonacion.class)
        .setParameter(
            "estado",
            EstadoDonacion.ASIGNACION_REALIZADA.name()
        )
        .getResultList();
  }

  public void pendientes(Context ctx) {
    String id;

    try {
      id = UUID.fromString(
          Objects.toString(
              ctx.queryParam("idDonacion"),
              ""
          )
      ).toString();
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse(
          "Indicá la donación que querés planificar."
      );
    }

    var candidatas = asignadas().stream()
        .filter(a ->
            a.getDonacion().getIdAsString().equals(id)
        )
        .toList();

    if (candidatas.size() != 1) {
      throw new ConflictResponse(
          "La donación no tiene una asignación pendiente única. "
              + "Recargá la página y revisá su estado."
      );
    }

    var asignacion = candidatas.get(0);
    var donacion = asignacion.getDonacion();
    var entidad = asignacion.getEntidad();

    var fila = new HashMap<String, Object>();

    fila.put("id", donacion.getIdAsString());
    fila.put("descripcion", donacion.getDescripcionGeneral());
    fila.put("entidad", entidad.getRazonSocial());
    fila.put("direccion", entidad.getDireccion());
    fila.put("cantidad", donacion.getCantidad());

    fila.put(
        "unidad",
        Objects.toString(donacion.getUnidadMedida(), "")
    );

    fila.put(
        "ubicada",
        coordenada(entidad.getLatitud(), 90)
            && coordenada(entidad.getLongitud(), 180)
    );

    ctx.header("Cache-Control", "no-store").json(Map.of(
        "donacion", fila,
        "deposito",
        System.getenv().getOrDefault(
            "DEPOSITO_DIRECCION",
            "Depósito de DonaTrack"
        )
    ));
  }

  public void planificar(Context ctx) throws IOException {
    Pedido pedido = ctx.bodyAsClass(Pedido.class);

    if (pedido == null
        || pedido.cargas() == null
        || pedido.cargas().size() != 1) {
      throw new BadRequestResponse(
          "La planificación debe corresponder "
              + "a la donación asignada."
      );
    }

    double latitud = coordenadaDeposito(
        "DEPOSITO_LATITUD", 90
    );

    double longitud = coordenadaDeposito(
        "DEPOSITO_LONGITUD", 180
    );

    var disponibles =
        new HashMap<String, AsignacionDonacion>();

    for (var asignacion : asignadas()) {
      String id = asignacion.getDonacion().getIdAsString();

      if (disponibles.put(id, asignacion) != null) {
        throw new ConflictResponse(
            "Hay más de una asignación para la misma donación."
        );
      }
    }

    for (var entrada : pedido.cargas().entrySet()) {
      var asignacion = disponibles.get(entrada.getKey());

      if (asignacion == null) {
        throw new ConflictResponse(
            "La donación cambió de estado. Recargá la página "
                + "y consultá Camiones antes de reintentar."
        );
      }

      var entidad = asignacion.getEntidad();

      if (!coordenada(entidad.getLatitud(), 90)
          || !coordenada(entidad.getLongitud(), 180)) {
        throw new ConflictResponse(
            "La dirección de la entidad todavía no tiene "
                + "una ubicación confirmada en el mapa."
        );
      }

      var carga = entrada.getValue();

      if (carga == null
          || !positivo(carga.pesoKg())
          || !positivo(carga.volumenM3())) {
        throw new BadRequestResponse(
            "La carga requiere peso y volumen positivos."
        );
      }
    }

    reenviar(
        ctx,
        "/planificaciones",
        json.writeValueAsString(Map.of(
            "latitudDeposito", latitud,
            "longitudDeposito", longitud,
            "cargas", pedido.cargas()
        )),
        300
    );
  }

  public void iniciar(Context ctx) throws IOException {
    String id;

    try {
      id = UUID.fromString(
          ctx.pathParam("idRuta")
      ).toString();
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("ID de ruta inválido.");
    }

    reenviar(
        ctx,
        "/rutas/id/" + id + "/iniciar",
        null,
        60
    );
  }

  private void reenviar(
      Context ctx,
      String ruta,
      String cuerpo,
      int segundos
  ) throws IOException {

    var pedido = HttpRequest.newBuilder(
            URI.create(logistica + ruta)
        )
        .timeout(Duration.ofSeconds(segundos))
        .header("Accept", "application/json")
        .header(
            "Content-Type",
            "application/json; charset=utf-8"
        )
        .POST(
            cuerpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(
                cuerpo,
                StandardCharsets.UTF_8
            )
        );

    try {
      var respuesta = http.send(
          pedido.build(),
          HttpResponse.BodyHandlers.ofString(
              StandardCharsets.UTF_8
          )
      );

      if (respuesta.statusCode() / 100 != 2) {
        String mensaje = respuesta.body();

        try {
          var nodo = json.readTree(mensaje);

          if (nodo != null && nodo.hasNonNull("mensaje")) {
            mensaje = nodo.get("mensaje").asText();
          } else if (nodo != null && nodo.hasNonNull("title")) {
            mensaje = nodo.get("title").asText();
          }
        } catch (IOException ignored) {
        }

        if (respuesta.statusCode() >= 500
            || mensaje.isBlank()
            || mensaje.length() > 500
            || mensaje.startsWith("<")) {
          mensaje =
              "Logística no pudo completar la operación. "
                  + "Revisá su consola y consultá Camiones "
                  + "antes de reintentar.";
        }

        ctx.status(
            respuesta.statusCode() >= 500
                ? 502
                : respuesta.statusCode()
        ).json(Map.of("mensaje", mensaje));

        return;
      }

      if (ruta.equals("/planificaciones")) {
        ctx.json(json.readTree(respuesta.body()));
      } else {
        ctx.json(Map.of(
            "mensaje",
            "Traslado iniciado. Las entidades ya pueden "
                + "confirmar la recepción."
        ));
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      throw new ServiceUnavailableResponse(
          "Operación interrumpida. Consultá Camiones "
              + "antes de reintentar."
      );
    } catch (IOException e) {
      throw new BadGatewayResponse(
          "No se pudo confirmar la operación. "
              + "Puede haberse procesado: recargá y consultá "
              + "Camiones antes de reintentar."
      );
    }
  }

  private static boolean coordenada(Double n, int limite) {
    return n != null
        && Double.isFinite(n)
        && Math.abs(n) <= limite;
  }

  private static boolean positivo(Double n) {
    return n != null
        && Double.isFinite(n)
        && n > 0;
  }

  private static double coordenadaDeposito(
      String variable,
      int limite
  ) {
    try {
      double valor = Double.parseDouble(
          Objects.toString(
              System.getenv(variable),
              ""
          ).trim()
      );

      if (!coordenada(valor, limite)) {
        throw new NumberFormatException();
      }

      return valor;
    } catch (NumberFormatException e) {
      throw new ServiceUnavailableResponse(
          "La ubicación del depósito no está configurada "
              + "correctamente en el servidor."
      );
    }
  }
}