package org.example.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ClienteDonaciones {
  private final HttpClient http;
  private final ObjectMapper json;
  private final String baseUrl;

  public ClienteDonaciones(String baseUrl) {
    this(HttpClient.newHttpClient(), new ObjectMapper(), baseUrl);
  }

  public ClienteDonaciones(HttpClient http, ObjectMapper json, String baseUrl) {
    this.http = http;
    this.json = json;
    this.baseUrl = baseUrl.replaceAll("/+$", "");
  }

  public Optional<AsignacionDisponible> buscarAsignacion(String idAsignacion)
      throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(
        URI.create(baseUrl + "/interno/asignaciones/" + idAsignacion)).GET().build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() == 404) return Optional.empty();
    if (response.statusCode() != 200) {
      throw new IOException("Donaciones respondio " + response.statusCode());
    }
    return Optional.of(json.readValue(response.body(), AsignacionDisponible.class));
  }

  public boolean existeDonacion(String idDonacion) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(
        URI.create(baseUrl + "/interno/donaciones/" + idDonacion + "/existe"))
        .GET().build();
    HttpResponse<Void> response = http.send(request, HttpResponse.BodyHandlers.discarding());
    if (response.statusCode() == 204) return true;
    if (response.statusCode() == 404) return false;
    throw new IOException("Donaciones respondio " + response.statusCode());
  }

  public List<AsignacionDisponible> listarAsignaciones()
      throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(
        URI.create(baseUrl + "/interno/asignaciones")).GET().build();
    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("Donaciones respondio " + response.statusCode());
    }
    return Arrays.asList(json.readValue(response.body(), AsignacionDisponible[].class));
  }

}
