package org.example.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.*;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

public class AdminCamionesController {

  private static final String BASE = "/administracion/camiones";

  private final String logistica = System.getenv()
      .getOrDefault("LOGISTICA_URL", "http://localhost:8082")
      .replaceAll("/+$", "");

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  private final ObjectMapper json = new ObjectMapper();

  public void index(Context ctx) {
    String q = texto(ctx.queryParam("q"));
    String estado = texto(ctx.queryParam("estado"));
    String pagina = Objects.toString(
        ctx.queryParam("pagina"), "1"
    );

    Map<String, Object> modelo = new HashMap<>();

    modelo.put("q", q);

    modelo.put("estados", List.of(
        opcion("", "Todos", estado),
        opcion("DISPONIBLE", "Disponible", estado),
        opcion("ASIGNADO", "Con ruta asignada", estado),
        opcion("EN_RUTA", "En ruta", estado),
        opcion("NO_DISPONIBLE", "No disponible", estado)
    ));

    try {
      var respuesta = llamar(
          "GET",
          "/gestion/camiones?q=" + codificar(q)
              + "&estado=" + codificar(estado)
              + "&pagina=" + codificar(pagina),
          null
      );

      if (respuesta.statusCode() != 200) {
        ctx.status(
            respuesta.statusCode() >= 500
                ? 502
                : respuesta.statusCode()
        );

        modelo.put("error", mensaje(respuesta.body()));
      } else {
        modelo.putAll(
            json.readValue(
                respuesta.body(),
                new TypeReference<Map<String, Object>>() {}
            )
        );

        int actual = ((Number) modelo.get("pagina")).intValue();
        long paginas = ((Number) modelo.get("paginas")).longValue();

        modelo.put("hayPaginacion", paginas > 1);

        String enlace = BASE
            + "?q=" + codificar(q)
            + "&estado=" + codificar(estado)
            + "&pagina=";

        if (actual > 1) {
          modelo.put("anterior", enlace + (actual - 1));
        }

        if (actual < paginas) {
          modelo.put("siguiente", enlace + (actual + 1));
        }
      }
    } catch (HttpResponseException | IOException e) {
      ctx.status(502);

      modelo.put(
          "error",
          "No se pudo consultar la flota. "
              + "Verificá que Logística esté levantada."
      );
    }

    ctx.header("Cache-Control", "no-store")
        .render("adminCamiones.hbs", modelo);
  }

  public void guardar(Context ctx) throws IOException {
    String patente = ctx.pathParamMap().get("patente");

    Map<String, Object> datos = new HashMap<>();

    datos.put(
        "patente",
        patente == null
            ? texto(ctx.formParam("patente"))
            : patente
    );

    datos.put("capacidadCarga", numero(ctx, "kg"));
    datos.put("capacidadVolumen", numero(ctx, "m3"));
    datos.put("altura", numero(ctx, "h"));
    datos.put("disponible", disponibilidad(ctx));

    reenviar(
        ctx,
        llamar(
            patente == null ? "POST" : "PUT",
            "/gestion/camiones"
                + (patente == null
                ? ""
                : "/" + codificar(patente)),
            json.writeValueAsString(datos)
        )
    );
  }

  public void cambiarDisponibilidad(Context ctx)
      throws IOException {

    reenviar(
        ctx,
        llamar(
            "PUT",
            "/gestion/camiones/"
                + codificar(ctx.pathParam("patente"))
                + "/disponibilidad",
            json.writeValueAsString(
                Map.of("disponible", disponibilidad(ctx))
            )
        )
    );
  }

  private HttpResponse<String> llamar(
      String metodo,
      String ruta,
      String cuerpo
  ) {
    var pedido = HttpRequest.newBuilder(
            URI.create(logistica + ruta)
        )
        .timeout(Duration.ofSeconds(10))
        .header("Accept", "application/json");

    if (cuerpo == null) {
      pedido.method(
          metodo,
          HttpRequest.BodyPublishers.noBody()
      );
    } else {
      pedido.header(
              "Content-Type",
              "application/json; charset=utf-8"
          )
          .method(
              metodo,
              HttpRequest.BodyPublishers.ofString(
                  cuerpo,
                  StandardCharsets.UTF_8
              )
          );
    }

    try {
      return http.send(
          pedido.build(),
          HttpResponse.BodyHandlers.ofString(
              StandardCharsets.UTF_8
          )
      );
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      throw new ServiceUnavailableResponse(
          "Comunicación interrumpida. "
              + "Recargá la lista antes de reintentar."
      );
    } catch (IOException e) {
      throw new BadGatewayResponse(
          "No se pudo confirmar la operación con Logística. "
              + "Recargá la lista antes de reintentar."
      );
    }
  }

  private void reenviar(
      Context ctx,
      HttpResponse<String> respuesta
  ) {
    int codigo = respuesta.statusCode();

    if (codigo >= 200 && codigo < 300) {
      ctx.status(codigo)
          .contentType("application/json")
          .result(respuesta.body());
    } else {
      ctx.status(codigo >= 500 ? 502 : codigo)
          .json(Map.of(
              "mensaje",
              mensaje(respuesta.body())
          ));
    }
  }

  private String mensaje(String cuerpo) {
    try {
      var nodo = json.readTree(cuerpo);

      if (nodo != null && nodo.hasNonNull("mensaje")) {
        return nodo.get("mensaje").asText();
      }

      if (nodo != null && nodo.hasNonNull("title")) {
        return nodo.get("title").asText();
      }
    } catch (IOException ignored) {
    }

    return !cuerpo.isBlank()
        && !cuerpo.stripLeading().startsWith("<")
        && cuerpo.length() < 500
        ? cuerpo
        : "Logística no pudo completar la operación.";
  }

  private static boolean disponibilidad(Context ctx) {
    String valor = ctx.formParam("disponible");

    if (!List.of("true", "false")
        .contains(Objects.toString(valor, ""))) {
      throw new BadRequestResponse(
          "Disponibilidad inválida."
      );
    }

    return Boolean.parseBoolean(valor);
  }

  private static double numero(Context ctx, String campo) {
    try {
      double n = Double.parseDouble(
          texto(ctx.formParam(campo))
      );

      if (!Double.isFinite(n) || n <= 0) {
        throw new NumberFormatException();
      }

      return n;
    } catch (NumberFormatException e) {
      throw new BadRequestResponse(
          "Ingresá medidas positivas y válidas."
      );
    }
  }

  private static Map<String, Object> opcion(
      String valor,
      String nombre,
      String actual
  ) {
    return Map.of(
        "valor", valor,
        "nombre", nombre,
        "elegido", valor.equals(actual)
    );
  }

  private static String texto(String s) {
    return Objects.toString(s, "").trim();
  }

  private static String codificar(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8)
        .replace("+", "%20");
  }

  public void eliminar(Context ctx) {
    reenviar(
        ctx,
        llamar(
            "DELETE",
            "/gestion/camiones/"
                + codificar(ctx.pathParam("patente")),
            null
        )
    );
  }
}