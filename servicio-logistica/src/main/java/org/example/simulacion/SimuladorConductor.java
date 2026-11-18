package org.example.simulacion;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class SimuladorConductor {

  private static final String BASE = "http://localhost:8082";
  private static final long INTERVALO_MS = 2000;
  private static final double VELOCIDAD_KMH = 30;

  // 1 = tiempo real; 10 = demostración acelerada.
  private static final double ACELERACION = 10;

  private static final ObjectMapper JSON = new ObjectMapper();

  private static final HttpClient HTTP = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  private record Punto(double lat, double lon) {}

  public static void main(String[] args) {
    Scanner consola = new Scanner(System.in);

    try {
      System.out.println("SIMULACIÓN LOCAL — usar rutas de prueba");
      System.out.print("ID de ruta iniciada: ");

      String id = UUID.fromString(
          consola.nextLine().trim()
      ).toString();

      String token = System.getenv("PLANIFICACION_TOKEN");

      if (token == null || token.isBlank()) {
        System.out.print(
            "Pegá el PLANIFICACION_TOKEN que ya usa Logística: "
        );
        token = consola.nextLine().trim();
      }

      if (token.isBlank()) {
        throw new IllegalArgumentException("Falta la credencial.");
      }

      JsonNode ruta = solicitar(
          "/integracion/rutas/" + id + "/recorrido",
          token,
          null
      );

      String patente = ruta.path("patente").asText();
      List<Punto> puntos = decodificar(
          ruta.path("geometria").asText()
      );

      if (patente.isBlank()) {
        throw new IOException("La respuesta no contiene patente.");
      }

      double[] distancias = new double[puntos.size()];

      for (int i = 1; i < puntos.size(); i++) {
        distancias[i] = distancias[i - 1]
            + distancia(puntos.get(i - 1), puntos.get(i));
      }

      double total = distancias[distancias.length - 1];

      System.out.printf(
          "Camión %s · %.2f km · aceleración %.0fx%n",
          patente, total / 1000, ACELERACION
      );

      System.out.println("Presioná Stop para detener el simulador.");

      long inicio = System.nanoTime();
      int segmento = 0;

      while (!Thread.currentThread().isInterrupted()) {
        double segundos =
            (System.nanoTime() - inicio) / 1_000_000_000.0;

        double avance = Math.min(
            total,
            segundos * VELOCIDAD_KMH / 3.6 * ACELERACION
        );

        while (segmento < puntos.size() - 2
            && distancias[segmento + 1] <= avance) {
          segmento++;
        }

        double longitud = distancias[segmento + 1]
            - distancias[segmento];

        double t = longitud == 0 ? 1 : Math.max(
            0,
            Math.min(1, (avance - distancias[segmento]) / longitud)
        );

        Punto a = puntos.get(segmento);
        Punto b = puntos.get(segmento + 1);

        double lat = a.lat() + (b.lat() - a.lat()) * t;
        double lon = a.lon() + (b.lon() - a.lon()) * t;
        boolean terminado = avance >= total;

        JsonNode respuesta = solicitar(
            "/camiones/ubicacion",
            token,
            Map.of(
                "idRuta", id,
                "patente", patente,
                "latitud", lat,
                "longitud", lon,
                "velocidadKmh", terminado ? 0 : VELOCIDAD_KMH,
                "fechaYHora", Instant.now().toString()
            )
        );

        System.out.printf(
            "[SIMULADO] %.5f, %.5f · %.0f%% · %s%n",
            lat,
            lon,
            total == 0 ? 100 : avance * 100 / total,
            respuesta.path("actualizado").asBoolean()
                ? "Registrado"
                : respuesta.path("motivo").asText()
        );

        if (terminado) {
          System.out.println(
              "Recorrido terminado. Las recepciones "
                  + "siguen a cargo de las entidades."
          );
          break;
        }

        Thread.sleep(INTERVALO_MS);
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      System.out.println("Simulación detenida.");
    } catch (Exception e) {
      System.err.println("Simulación detenida: " + e.getMessage());
    }
  }

  private static JsonNode solicitar(
      String ruta,
      String token,
      Object cuerpo
  ) throws IOException, InterruptedException {

    var builder = HttpRequest.newBuilder(URI.create(BASE + ruta))
        .timeout(Duration.ofSeconds(15))
        .header("Authorization", "Bearer " + token)
        .header("Accept", "application/json");

    if (cuerpo == null) {
      builder.GET();
    } else {
      builder.header("Content-Type", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(
              JSON.writeValueAsString(cuerpo)
          ));
    }

    var respuesta = HTTP.send(
        builder.build(),
        HttpResponse.BodyHandlers.ofString()
    );

    if (respuesta.statusCode() / 100 != 2) {
      String mensaje = "HTTP " + respuesta.statusCode();

      try {
        JsonNode error = JSON.readTree(respuesta.body());

        for (String campo : List.of(
            "mensaje", "detail", "title", "message"
        )) {
          if (error != null && error.hasNonNull(campo)) {
            mensaje += ": " + error.path(campo).asText();
            break;
          }
        }
      } catch (IOException ignored) {}

      throw new IOException(mensaje);
    }

    return JSON.readTree(respuesta.body());
  }

  private static List<Punto> decodificar(String geometria) {
    var puntos = new ArrayList<Punto>();
    int[] indice = {0};
    long lat = 0;
    long lon = 0;

    while (indice[0] < geometria.length()) {
      lat += diferencia(geometria, indice);
      lon += diferencia(geometria, indice);

      double latitud = lat / 100000.0;
      double longitud = lon / 100000.0;

      if (Math.abs(latitud) > 90 || Math.abs(longitud) > 180) {
        throw new IllegalArgumentException("Geometría inválida.");
      }

      puntos.add(new Punto(latitud, longitud));
    }

    if (puntos.size() < 2) {
      throw new IllegalArgumentException("La ruta no tiene recorrido.");
    }

    return puntos;
  }

  private static long diferencia(String texto, int[] indice) {
    long resultado = 0;
    int desplazamiento = 0;
    int valor;

    do {
      if (indice[0] >= texto.length() || desplazamiento > 30) {
        throw new IllegalArgumentException("Geometría incompleta.");
      }

      valor = texto.charAt(indice[0]++) - 63;

      if (valor < 0 || valor > 63) {
        throw new IllegalArgumentException("Geometría inválida.");
      }

      resultado |= (long) (valor & 31) << desplazamiento;
      desplazamiento += 5;

    } while (valor >= 32);

    return (resultado & 1) == 1
        ? -((resultado >> 1) + 1)
        : resultado >> 1;
  }

  private static double distancia(Punto a, Punto b) {
    double lat1 = Math.toRadians(a.lat());
    double lat2 = Math.toRadians(b.lat());
    double dLat = lat2 - lat1;
    double dLon = Math.toRadians(b.lon() - a.lon());

    double h = Math.pow(Math.sin(dLat / 2), 2)
        + Math.cos(lat1) * Math.cos(lat2)
        * Math.pow(Math.sin(dLon / 2), 2);

    return 6371000 * 2 * Math.asin(
        Math.sqrt(Math.max(0, Math.min(1, h)))
    );
  }
}