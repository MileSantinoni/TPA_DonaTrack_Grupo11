package org.example.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ClienteGeocodificacion {

  private final String apiKey;
  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .build();

  private final ObjectMapper json = new ObjectMapper();

  public ClienteGeocodificacion(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("Falta configurar HEIGIT_API_KEY");
    }

    this.apiKey = apiKey.trim();
  }

  public List<Candidato> buscar(String direccion)
      throws IOException, InterruptedException {

    if (direccion == null || direccion.isBlank()) {
      throw new IllegalArgumentException(
          "La entidad debe tener una direccion completa"
      );
    }

    String texto = URLEncoder.encode(
        direccion.trim(), StandardCharsets.UTF_8
    );

    URI uri = URI.create(
        "https://api.heigit.org/pelias/v1/search"
            + "?text=" + texto
            + "&boundary.country=ARG"
            + "&layers=address"
            + "&size=5"
    );

    HttpRequest request = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(30))
        .header("Authorization", apiKey)
        .header("Accept", "application/json")
        .GET()
        .build();

    HttpResponse<String> response = http.send(
        request, HttpResponse.BodyHandlers.ofString()
    );

    if (response.statusCode() != 200) {
      throw new IOException(
          "Geocodificacion respondio HTTP " + response.statusCode()
      );
    }

    JsonNode raiz = json.readTree(response.body());

    if (raiz == null || !raiz.path("features").isArray()) {
      throw new IOException("Respuesta de geocodificacion invalida");
    }

    List<Candidato> candidatos = new ArrayList<>();

    for (JsonNode feature : raiz.path("features")) {
      JsonNode geometry = feature.path("geometry");
      JsonNode coordenadas = geometry.path("coordinates");

      if (!"Point".equals(geometry.path("type").asText())
          || !coordenadas.isArray()
          || coordenadas.size() < 2
          || !coordenadas.get(0).isNumber()
          || !coordenadas.get(1).isNumber()) {
        throw new IOException("El proveedor devolvio coordenadas invalidas");
      }

      double longitud = coordenadas.get(0).asDouble();
      double latitud = coordenadas.get(1).asDouble();

      if (!Double.isFinite(latitud)
          || !Double.isFinite(longitud)
          || latitud < -90 || latitud > 90
          || longitud < -180 || longitud > 180) {
        throw new IOException("Coordenadas fuera de rango");
      }

      candidatos.add(new Candidato(
          feature.path("properties").path("label").asText(),
          latitud,
          longitud
      ));
    }

    return List.copyOf(candidatos);
  }

  public record Candidato(
      String descripcion,
      double latitud,
      double longitud
  ) {
  }
}