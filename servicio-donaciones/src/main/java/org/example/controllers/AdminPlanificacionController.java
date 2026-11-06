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
      Double latitudDeposito,
      Double longitudDeposito,
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
    var filas = new ArrayList<Map<String, Object>>();
    var ids = new HashSet<String>();

    for (var a : asignadas()) {
      var d = a.getDonacion();
      var e = a.getEntidad();

      if (!ids.add(d.getIdAsString())) {
        throw new ConflictResponse(
            "Hay más de una asignación para la misma donación."
        );
      }

      var fila = new HashMap<String, Object>();

      fila.put("id", d.getIdAsString());
      fila.put("descripcion", d.getDescripcionGeneral());
      fila.put("entidad", e.getRazonSocial());
      fila.put("direccion", e.getDireccion());

      fila.put(
          "ubicada",
          e.getLatitud() != null && e.getLongitud() != null
      );

      fila.put("cantidad", d.getCantidad());
      fila.put(
          "unidad",
          Objects.toString(d.getUnidadMedida(), "")
      );

      filas.add(fila);
    }

    ctx.header("Cache-Control", "no-store").json(Map.of(
        "donaciones", filas,
        "latitud",
        System.getenv().getOrDefault("DEPOSITO_LATITUD", ""),
        "longitud",
        System.getenv().getOrDefault("DEPOSITO_LONGITUD", "")
    ));
  }

  public void planificar(Context ctx) throws IOException {
    Pedido pedido = ctx.bodyAsClass(Pedido.class);

    if (pedido == null
        || !coordenada(pedido.latitudDeposito(), 90)
        || !coordenada(pedido.longitudDeposito(), 180)) {
      throw new BadRequestResponse(
          "Ingresá coordenadas válidas del depósito."
      );
    }

    if (pedido.cargas() == null
        || pedido.cargas().isEmpty()
        || pedido.cargas().size() > 50) {
      throw new BadRequestResponse(
          "Seleccioná entre 1 y 50 donaciones por planificación."
      );
    }

    var disponibles =
        new HashMap<String, AsignacionDonacion>();

    for (var a : asignadas()) {
      if (disponibles.put(
          a.getDonacion().getIdAsString(), a
      ) != null) {
        throw new ConflictResponse(
            "Hay más de una asignación para la misma donación."
        );
      }
    }

    for (var entrada : pedido.cargas().entrySet()) {
      var a = disponibles.get(entrada.getKey());

      if (a == null) {
        throw new ConflictResponse(
            "Una donación cambió de estado. Recargá la página "
                + "y consultá Camiones antes de reintentar."
        );
      }

      if (a.getEntidad().getLatitud() == null
          || a.getEntidad().getLongitud() == null) {
        throw new ConflictResponse(
            "Faltan las coordenadas de una entidad destinataria."
        );
      }

      var carga = entrada.getValue();

      if (carga == null
          || !positivo(carga.pesoKg())
          || !positivo(carga.volumenM3())) {
        throw new BadRequestResponse(
            "Todas las donaciones seleccionadas requieren "
                + "peso y volumen positivos."
        );
      }
    }

    reenviar(
        ctx,
        "/planificaciones",
        json.writeValueAsString(pedido),
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
}