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

    com.fasterxml.jackson.databind.node.ObjectNode cuerpo =
        json.valueToTree(solicitud);

    cuerpo.putObject("options").put("g", true);

    HttpRequest request = HttpRequest.newBuilder(endpoint)
        .timeout(Duration.ofSeconds(60))
        .header("Authorization", apiKey)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(
            json.writeValueAsString(cuerpo)
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

    for (JsonNode ruta : resultado.path("routes")) {
      JsonNode geometria = ruta.path("geometry");

      if (!geometria.isTextual() || geometria.asText().isBlank()) {
        throw new IOException(
            "HeiGIT devolvió una ruta sin geometría. "
                + "No se puede guardar el recorrido."
        );
      }
    }

    agregarRecorridosPorEntrega(resultado, solicitud);
    // Conservamos ambas listas, incluyendo las entregas sin asignar.
    return resultado;
  }

  private void agregarRecorridosPorEntrega(
      JsonNode resultado,
      Solicitud solicitud
  ) throws IOException, InterruptedException {

    for (JsonNode ruta : resultado.path("routes")) {
      int idVehiculo = ruta.path("vehicle").asInt();

      Vehiculo vehiculo = solicitud.vehicles().stream()
          .filter(v -> v.id() == idVehiculo)
          .findFirst()
          .orElseThrow(() ->
              new IOException("Vehículo desconocido en la ruta.")
          );

      // Conservamos exactamente el orden decidido por VROOM.
      var pasos = new java.util.ArrayList<
          com.fasterxml.jackson.databind.node.ObjectNode
          >();

      var cuerpo = json.createObjectNode();
      var coordenadas = cuerpo.putArray("coordinates");

      for (JsonNode paso : ruta.path("steps")) {
        String tipo = paso.path("type").asText();

        if (!java.util.Set.of("start", "job", "end").contains(tipo)) {
          throw new IOException("Tipo de parada no soportado: " + tipo);
        }

        JsonNode ubicacion = paso.path("location");

        if (!(paso instanceof
            com.fasterxml.jackson.databind.node.ObjectNode objeto)
            || !ubicacion.isArray()
            || ubicacion.size() != 2
            || !ubicacion.get(0).isNumber()
            || !ubicacion.get(1).isNumber()) {
          throw new IOException("Parada sin coordenadas válidas.");
        }

        pasos.add(objeto);
        coordenadas.add(ubicacion.deepCopy());
      }

      if (pasos.size() < 2 || pasos.size() > 50) {
        throw new IOException(
            "La ruta debe tener entre 2 y 50 puntos para Directions."
        );
      }

      cuerpo.put("instructions", false);
      cuerpo.put("geometry", true);
      cuerpo.put("elevation", false);

      if (!"driving-hgv".equals(vehiculo.profile())) {
        throw new IOException(
            "El seguimiento espera el perfil driving-hgv."
        );
      }

      var request = HttpRequest.newBuilder(
              URI.create(
                  "https://api.heigit.org/openrouteservice"
                      + "/v2/directions/driving-hgv/json"
              )
          )
          .timeout(Duration.ofSeconds(60))
          .header("Authorization", apiKey)
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(
              json.writeValueAsString(cuerpo)
          ))
          .build();

      var response = http.send(
          request,
          HttpResponse.BodyHandlers.ofString()
      );

      if (response.statusCode() != 200) {
        throw new IOException(
            "Directions de HeiGIT respondió HTTP "
                + response.statusCode()
                + ". Revisá el acceso a Directions y la cuota."
        );
      }

      JsonNode respuesta = json.readTree(response.body());

      if (respuesta == null
          || !respuesta.path("routes").isArray()
          || respuesta.path("routes").size() != 1) {
        throw new IOException("Directions no devolvió una ruta válida.");
      }

      JsonNode detalle = respuesta.path("routes").get(0);
      JsonNode geometria = detalle.path("geometry");
      JsonNode indices = detalle.path("way_points");

      if (!geometria.isTextual()
          || geometria.asText().isBlank()
          || !indices.isArray()
          || indices.size() != pasos.size()) {
        throw new IOException(
            "Directions no devolvió la geometría y sus paradas."
        );
      }

      String completa = geometria.asText();

      // La ruta completa y los recorridos parciales usan la misma línea.
      ((com.fasterxml.jackson.databind.node.ObjectNode) ruta)
          .put("geometry", completa);

      int anterior = -1;

      for (int i = 0; i < pasos.size(); i++) {
        JsonNode indice = indices.get(i);

        if (!indice.isIntegralNumber()
            || !indice.canConvertToInt()
            || indice.asInt() < 0
            || indice.asInt() < anterior) {
          throw new IOException("Índices de parada inválidos.");
        }

        anterior = indice.asInt();

        if ("job".equals(pasos.get(i).path("type").asText())) {
          pasos.get(i).put(
              "geometriaHastaEntrega",
              prefijoGeometria(completa, anterior)
          );
        }
      }
    }
  }

  private static String prefijoGeometria(
      String geometria,
      int ultimoPunto
  ) throws IOException {

    int posicion = 0;

    // Una polyline codifica dos valores por punto: latitud y longitud.
    // Al conservar el prefijo, no hace falta recodificar la geometría.
    for (int punto = 0; punto <= ultimoPunto; punto++) {
      for (int componente = 0; componente < 2; componente++) {
        int grupos = 0;
        int valor;

        do {
          if (posicion >= geometria.length() || ++grupos > 7) {
            throw new IOException("Geometría incompleta o inválida.");
          }

          valor = geometria.charAt(posicion++) - 63;

          if (valor < 0 || valor > 63) {
            throw new IOException("Geometría inválida.");
          }
        } while (valor >= 32);
      }
    }

    return geometria.substring(0, posicion);
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