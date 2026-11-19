package org.example.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;

public class FotosPublicasController {

  private final Supplier<EntityManager> proveedor;

  private final ObjectMapper json = new ObjectMapper();

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  private final String logistica = System.getenv()
      .getOrDefault("LOGISTICA_URL", "http://localhost:8082")
      .replaceAll("/+$", "");

  private final Path carpeta = Path.of(
      System.getenv().getOrDefault(
          "DONATRACK_ARCHIVOS_DIR", "datos/recepciones"
      )
  ).toAbsolutePath().normalize();

  public FotosPublicasController(Supplier<EntityManager> proveedor) {
    this.proveedor = proveedor;
  }

  private String idPublicado(Context ctx) {
    UUID id;

    try {
      id = UUID.fromString(ctx.pathParam("id"));
    } catch (IllegalArgumentException e) {
      throw new NotFoundResponse("Entrega no encontrada.");
    }

    Donacion donacion = proveedor.get().find(Donacion.class, id);

    if (donacion == null
        || donacion.getEstadoActual() != EstadoDonacion.ENTREGADA) {
      throw new NotFoundResponse("Entrega no encontrada.");
    }

    return id.toString();
  }

  private List<String> archivosRegistrados(String id) {
    var pedido = HttpRequest.newBuilder(
            URI.create(
                logistica + "/interno/donaciones/" + id + "/seguimiento"
            )
        )
        .timeout(Duration.ofSeconds(8))
        .header("Accept", "application/json")
        .GET()
        .build();

    try {
      var respuesta = http.send(
          pedido, HttpResponse.BodyHandlers.ofString()
      );

      if (respuesta.statusCode() == 404) {
        throw new NotFoundResponse("Entrega no encontrada.");
      }

      if (respuesta.statusCode() != 200) {
        throw new BadGatewayResponse(
            "No se pudieron consultar las fotos."
        );
      }

      JsonNode datos = json.readTree(respuesta.body());

      if (datos == null
          || !"ENTREGADA".equals(datos.path("estadoEntrega").asText())) {
        throw new NotFoundResponse("Entrega no confirmada.");
      }

      String prefijo = "/mi-entidad/fotos/" + id + "/";
      List<String> archivos = new ArrayList<>();

      for (JsonNode foto : datos.path("fotos")) {
        if (!foto.isTextual()) continue;

        String referencia = foto.asText();
        if (!referencia.startsWith(prefijo)) continue;

        String nombre = referencia.substring(prefijo.length());

        if (nombre.matches("[a-f0-9]{64}\\.(jpg|png)")) {
          archivos.add(nombre);
        }
      }

      return archivos.stream().distinct().toList();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ServiceUnavailableResponse("Consulta interrumpida.");
    } catch (IOException e) {
      throw new BadGatewayResponse(
          "No se pudo conectar con las fotos de la entrega."
      );
    }
  }

  public void listar(Context ctx) {
    String id = idPublicado(ctx);

    var fotos = archivosRegistrados(id).stream()
        .map(nombre -> "/api/publico/entregas/" + id + "/fotos/" + nombre)
        .toList();

    ctx.header("Cache-Control", "no-store")
        .json(Map.of("fotos", fotos));
  }

  public void mostrar(Context ctx) throws IOException {
    String id = idPublicado(ctx);
    String nombre = ctx.pathParam("archivo");

    if (!nombre.matches("[a-f0-9]{64}\\.(jpg|png)")
        || !archivosRegistrados(id).contains(nombre)) {
      throw new NotFoundResponse("Foto no encontrada.");
    }

    Path archivo = carpeta.resolve(id).resolve(nombre).normalize();

    if (!archivo.startsWith(carpeta)
        || !Files.isRegularFile(archivo)) {
      throw new NotFoundResponse("Foto no encontrada.");
    }

    ctx.header("Cache-Control", "no-store");
    ctx.header("X-Content-Type-Options", "nosniff");
    ctx.contentType(
        nombre.endsWith(".png") ? "image/png" : "image/jpeg"
    );
    ctx.result(Files.readAllBytes(archivo));
  }
}