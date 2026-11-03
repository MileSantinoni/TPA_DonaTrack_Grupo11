package org.example.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public class ClienteHeiGit {

  private final HttpClient http;
  private final URI endpoint;
  private final String apiKey;
  private final ObjectMapper json = new ObjectMapper();

  public ClienteHeiGit(String apiKey) {
    this(
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build(),
        URI.create("https://api.heigit.org/vroom/v0"),
        apiKey
    );
  }

  public ClienteHeiGit(HttpClient http, URI endpoint, String apiKey) {
    this.http = Objects.requireNonNull(http);
    this.endpoint = Objects.requireNonNull(endpoint);

    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalArgumentException(
          "Falta configurar HEIGIT_API_KEY"
      );
    }

    this.apiKey = apiKey.trim();
  }

  public JsonNode optimizar(Solicitud solicitud)
      throws IOException, InterruptedException {

    Objects.requireNonNull(solicitud);

    if (solicitud.jobs() == null || solicitud.jobs().isEmpty()) {
      throw new IllegalArgumentException(
          "Debe indicar al menos una entrega"
      );
    }

    if (solicitud.vehicles() == null || solicitud.vehicles().isEmpty()) {
      throw new IllegalArgumentException(
          "Debe indicar al menos un vehiculo"
      );
    }

    HttpRequest request = HttpRequest.newBuilder(endpoint)
        .timeout(Duration.ofSeconds(60))
        .header("Authorization", apiKey)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(
            json.writeValueAsString(solicitud)
        ))
        .build();

    HttpResponse<String> response = http.send(
        request, HttpResponse.BodyHandlers.ofString()
    );

    if (response.statusCode() != 200) {
      throw new IOException(
          "HeiGIT respondio HTTP " + response.statusCode()
              + ". Revisar la clave, cuota y parametros de la solicitud."
      );
    }

    JsonNode resultado = json.readTree(response.body());

    if (resultado == null
        || !resultado.path("code").isIntegralNumber()
        || resultado.path("code").asInt() != 0) {
      throw new IOException(
          "HeiGIT no devolvio una optimizacion exitosa"
      );
    }

    if (!resultado.path("routes").isArray()
        || !resultado.path("unassigned").isArray()) {
      throw new IOException(
          "La respuesta de HeiGIT no contiene rutas y entregas sin asignar"
      );
    }

    // Conservamos ambas listas, incluyendo las entregas sin asignar.
    return resultado;
  }

  public record Solicitud(
      List<Trabajo> jobs,
      List<Vehiculo> vehicles
  ) {
  }

  public record Trabajo(
      int id,
      String description,
      List<Double> location,
      List<Integer> delivery,
      int service
  ) {
  }

  public record Vehiculo(
      int id,
      String description,
      String profile,
      List<Double> start,
      List<Double> end,
      List<Integer> capacity
  ) {
  }
}