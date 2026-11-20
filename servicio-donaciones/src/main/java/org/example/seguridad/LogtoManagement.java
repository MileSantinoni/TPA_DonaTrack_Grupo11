package org.example.seguridad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.ConflictResponse;
import io.javalin.http.ServiceUnavailableResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

public class LogtoManagement {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();

  private String token;
  private Instant vencimiento = Instant.EPOCH;

  public String issuer() {
    String issuer = variable("LOGTO_ISSUER").replaceAll("/+$", "");
    URI uri = URI.create(issuer);

    if (!"https".equals(uri.getScheme())
        || uri.getHost() == null
        || uri.getUserInfo() != null
        || uri.getQuery() != null
        || uri.getFragment() != null
        || !"/oidc".equals(uri.getPath())) {
      throw new ServiceUnavailableResponse(
          "Revisá LOGTO_ISSUER en el servidor."
      );
    }

    return issuer;
  }

  private String base() {
    String issuer = issuer();
    return issuer.substring(0, issuer.length() - "/oidc".length());
  }

  public String crearDonante(
      String email,
      String nombre,
      String operacion
  ) {
    /*
     * Permite retomar un alta si Logto creó la cuenta pero falló
     * el guardado local. No reutiliza cuentas ajenas a este flujo.
     */
    String marca = json(Map.of("donatrackAltaAdmin", operacion));

    JsonNode existentes = consultar(
        "/users?customData=" + codificar(marca)
    );

    if (!existentes.isArray() || existentes.size() > 1) {
      throw fallo();
    }

    if (existentes.size() == 1) {
      JsonNode usuario = existentes.get(0);
      String id = usuario.path("id").asText();

      if (!email.equalsIgnoreCase(
          usuario.path("primaryEmail").asText()
      )
          || !operacion.equals(
          usuario.path("customData")
              .path("donatrackAltaAdmin")
              .asText()
      )
          || usuario.path("isSuspended").asBoolean()
          || (!usuario.path("lastSignInAt").isNull()
          && !usuario.path("lastSignInAt").isMissingNode())) {
        throw new ConflictResponse(
            "La cuenta ya existe. Revisá su vinculación antes de continuar."
        );
      }

      JsonNode roles = consultar(
          "/users/" + codificar(id) + "/roles"
      );

      if (!roles.isArray() || !roles.isEmpty()) {
        throw new ConflictResponse(
            "La cuenta existente tiene roles y no se vinculará automáticamente."
        );
      }

      if (id.isBlank()) {
        throw fallo();
      }

      return id;
    }

    var respuesta = enviar(
        peticion(base() + "/api/users")
            .header("Authorization", "Bearer " + acceso())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                json(Map.of(
                    "primaryEmail", email,
                    "name", nombre,
                    "customData", Map.of(
                        "donatrackAltaAdmin", operacion
                    )
                ))
            ))
            .build()
    );

    if (respuesta.statusCode() == 400
        || respuesta.statusCode() == 409
        || respuesta.statusCode() == 422) {
      throw new ConflictResponse(
          "Logto no aceptó el alta. Revisá si el correo ya tiene "
              + "una cuenta; no se vinculó a otro usuario."
      );
    }

    verificar(respuesta);

    String id = leer(respuesta.body()).path("id").asText();

    if (id.isBlank()) {
      throw fallo();
    }

    return id;
  }

  private JsonNode consultar(String ruta) {
    var respuesta = enviar(
        peticion(base() + "/api" + ruta)
            .header("Authorization", "Bearer " + acceso())
            .GET()
            .build()
    );

    verificar(respuesta);

    return leer(respuesta.body());
  }

  private synchronized String acceso() {
    if (token != null && Instant.now().isBefore(vencimiento)) {
      return token;
    }

    String credenciales = Base64.getEncoder().encodeToString(
        (
            variable("LOGTO_M2M_APP_ID")
                + ":"
                + variable("LOGTO_M2M_APP_SECRET")
        ).getBytes(StandardCharsets.UTF_8)
    );

    var respuesta = enviar(
        peticion(issuer() + "/token")
            .header("Authorization", "Basic " + credenciales)
            .header(
                "Content-Type",
                "application/x-www-form-urlencoded"
            )
            .POST(HttpRequest.BodyPublishers.ofString(
                "grant_type=client_credentials"
                    + "&scope=all"
                    + "&resource=" + codificar(base() + "/api")
            ))
            .build()
    );

    verificar(respuesta);

    JsonNode datos = leer(respuesta.body());
    String nuevo = datos.path("access_token").asText();
    long segundos = datos.path("expires_in").asLong();

    if (nuevo.isBlank() || segundos <= 0) {
      throw fallo();
    }

    token = nuevo;
    vencimiento = Instant.now()
        .plusSeconds(Math.max(0, segundos - 30));

    return token;
  }

  private void verificar(HttpResponse<String> respuesta) {
    if (respuesta.statusCode() >= 200
        && respuesta.statusCode() < 300) {
      return;
    }

    if (respuesta.statusCode() == 401
        || respuesta.statusCode() == 403) {
      synchronized (this) {
        token = null;
      }

      throw new ServiceUnavailableResponse(
          "Revisá las credenciales M2M y el rol "
              + "Logto Management API access en el servidor."
      );
    }

    // No exponemos el cuerpo de respuesta ni las credenciales.
    throw fallo();
  }

  private HttpResponse<String> enviar(HttpRequest solicitud) {
    try {
      return http.send(
          solicitud,
          HttpResponse.BodyHandlers.ofString()
      );
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw fallo();
    } catch (IOException e) {
      throw fallo();
    }
  }

  private static HttpRequest.Builder peticion(String url) {
    return HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(20))
        .header("Accept", "application/json");
  }

  private static JsonNode leer(String valor) {
    try {
      JsonNode datos = JSON.readTree(valor);

      if (datos == null || datos.isNull()) {
        throw fallo();
      }

      return datos;
    } catch (IOException e) {
      throw fallo();
    }
  }

  private static String json(Object valor) {
    try {
      return JSON.writeValueAsString(valor);
    } catch (IOException e) {
      throw fallo();
    }
  }

  private static String variable(String nombre) {
    String valor = System.getenv(nombre);

    if (valor == null || valor.isBlank()) {
      throw new ServiceUnavailableResponse(
          "Falta configurar " + nombre + " en el servidor."
      );
    }

    return valor.trim();
  }

  private static String codificar(String valor) {
    return URLEncoder.encode(valor, StandardCharsets.UTF_8);
  }

  private static ServiceUnavailableResponse fallo() {
    return new ServiceUnavailableResponse(
        "No se pudo completar el alta en Logto. "
            + "Volvé a intentar con los mismos datos."
    );
  }
}