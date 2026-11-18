package org.example.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.example.dominio.logistica.EventoLogistico;

public class ClienteDonaciones
    implements org.example.dominio.logistica.Donaciones {

  private static final String CAMPOS_ASIGNACION = """
        idDonacion
        idEntidad
        razonSocial
        direccion
        telefono
        estadoDonacion
        latitud
        longitud
        """;

  private final HttpClient http;
  private final ObjectMapper json;
  private final URI endpoint;

  public ClienteDonaciones(String baseUrl) {
    this(
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build(),
        new ObjectMapper().findAndRegisterModules(),
        baseUrl
    );
  }

  public ClienteDonaciones(
      HttpClient http,
      ObjectMapper json,
      String baseUrl
  ) {
    this.http = http;

    this.json = json.copy()
        .findAndRegisterModules()
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    String base = baseUrl.replaceAll("/+$", "");

    this.endpoint = URI.create(
        base.endsWith("/graphql")
            ? base
            : base + "/graphql"
    );
  }

  public Optional<AsignacionDisponible> buscarAsignacion(
      String idAsignacion
  ) throws IOException, InterruptedException {

    String consulta = """
            query BuscarAsignacion($id: ID!) {
                asignacion(id: $id) {
                    %s
                }
            }
            """.formatted(CAMPOS_ASIGNACION);

    JsonNode resultado = ejecutar(
        consulta,
        Map.of("id", idAsignacion),
        "asignacion"
    );

    if (resultado.isNull()) {
      return Optional.empty();
    }

    return Optional.of(
        json.treeToValue(resultado, AsignacionDisponible.class)
    );
  }

  public boolean existeDonacion(String idDonacion)
      throws IOException, InterruptedException {

    JsonNode resultado = ejecutar(
        """
        query ExisteDonacion($id: ID!) {
            existeDonacion(id: $id)
        }
        """,
        Map.of("id", idDonacion),
        "existeDonacion"
    );

    if (!resultado.isBoolean()) {
      throw new IOException(
          "Donaciones devolvió una existencia inválida"
      );
    }

    return resultado.booleanValue();
  }

  public List<AsignacionDisponible> listarAsignaciones()
      throws IOException, InterruptedException {

    String consulta = """
            query ListarAsignaciones {
                asignaciones {
                    %s
                }
            }
            """.formatted(CAMPOS_ASIGNACION);

    JsonNode resultado = ejecutar(
        consulta,
        Map.of(),
        "asignaciones"
    );

    if (!resultado.isArray()) {
      throw new IOException(
          "Donaciones devolvió una lista de asignaciones inválida"
      );
    }

    return Arrays.asList(
        json.treeToValue(
            resultado,
            AsignacionDisponible[].class
        )
    );
  }

  @Override
  public List<org.example.dominio.logistica.Donaciones.Asignacion>
  listarDestinos() throws IOException, InterruptedException {

    return listarAsignaciones().stream()
        .map(a ->
            new org.example.dominio.logistica.Donaciones.Asignacion(
                a.idDonacion(),
                a.idEntidad(),
                a.razonSocial(),
                a.direccion(),
                a.telefono(),
                a.estadoDonacion()
            )
        )
        .toList();
  }

  @Override
  public void informar(EventoLogistico evento)
      throws IOException, InterruptedException {

    JsonNode resultado = ejecutar(
        """
        mutation RegistrarEvento($input: EventoLogisticoInput!) {
            registrarEvento(input: $input)
        }
        """,
        Map.of("input", evento),
        "registrarEvento"
    );

    if (!resultado.isBoolean() || !resultado.booleanValue()) {
      throw new IOException(
          "Donaciones no confirmó la recepción del evento"
      );
    }
  }

  private JsonNode ejecutar(
      String documento,
      Map<String, Object> variables,
      String campo
  ) throws IOException, InterruptedException {

    String token = System.getenv("PLANIFICACION_TOKEN");

    if (token == null || token.isBlank()) {
      // Es un problema de configuración, no un rechazo del evento.
      throw new IOException(
          "Falta configurar PLANIFICACION_TOKEN en Logística"
      );
    }

    String cuerpo = json.writeValueAsString(
        Map.of(
            "query", documento,
            "variables", variables
        )
    );

    HttpRequest request = HttpRequest.newBuilder(endpoint)
        .timeout(Duration.ofSeconds(10))
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("X-Planificacion-Token", token)
        .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
        .build();

    HttpResponse<String> response = http.send(
        request,
        HttpResponse.BodyHandlers.ofString()
    );

    if (response.statusCode() != 200) {
      throw new IOException(
          "Donaciones respondió HTTP " + response.statusCode()
      );
    }

    JsonNode raiz = json.readTree(response.body());

    if (raiz == null || !raiz.isObject()) {
      throw new IOException(
          "Donaciones devolvió una respuesta GraphQL inválida"
      );
    }

    JsonNode errores = raiz.get("errors");

    if (errores != null) {
      if (!errores.isArray()) {
        throw new IOException(
            "El campo errors de GraphQL es inválido"
        );
      }

      if (!errores.isEmpty()) {
        procesarErrores(errores);
      }
    }

    JsonNode datos = raiz.get("data");

    if (datos == null
        || !datos.isObject()
        || !datos.has(campo)) {

      throw new IOException(
          "Falta data." + campo + " en la respuesta de Donaciones"
      );
    }

    return datos.get(campo);
  }

  private void procesarErrores(JsonNode errores)
      throws IOException {

    // Un error técnico conserva el evento para un posible reintento.
    // Los códigos 400 y 409 son rechazos explícitos del dominio.
    for (JsonNode error : errores) {
      int estado = error.path("extensions")
          .path("status")
          .asInt(502);

      if (estado != 400 && estado != 409) {
        throw new IOException(
            "Falló la operación GraphQL: "
                + error.path("message")
                .asText("Error sin detalle")
        );
      }
    }

    JsonNode primero = errores.get(0);

    String mensaje = primero.path("message")
        .asText("Operación rechazada por Donaciones");

    int estado = primero.path("extensions")
        .path("status")
        .asInt();

    if (estado == 400) {
      throw new IllegalArgumentException(mensaje);
    }

    throw new IllegalStateException(mensaje);
  }
}