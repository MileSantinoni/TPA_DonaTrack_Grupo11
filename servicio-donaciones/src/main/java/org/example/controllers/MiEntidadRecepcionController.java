package org.example.controllers;

import io.javalin.http.*;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import javax.imageio.ImageIO;

public class MiEntidadRecepcionController {

  private static final int MAX = 5 * 1024 * 1024;

  private final MiEntidadController panel;

  private final Path carpeta = Path.of(
      System.getenv().getOrDefault(
          "DONATRACK_ARCHIVOS_DIR",
          "datos/recepciones"
      )
  ).toAbsolutePath().normalize();

  private record Foto(String nombre, byte[] bytes) {}

  public MiEntidadRecepcionController(MiEntidadController panel) {
    this.panel = panel;
  }

  public void confirmar(Context ctx) throws IOException {
    String id = panel.donacionPropia(
        ctx.pathParam("id")
    ).getIdAsString();

    if (!"on".equals(ctx.formParam("confirmacion"))) {
      throw new BadRequestResponse(
          "Confirmá que recibiste los bienes."
      );
    }

    var seguimiento = panel.seguimientoDatos(id);
    String estado = seguimiento.path("estadoEntrega").asText();

    if (!List.of("EN_TRASLADO", "ENTREGADA").contains(estado)) {
      throw new ConflictResponse(
          "La entrega no está en traslado."
      );
    }

    var archivos = ctx.uploadedFiles("fotos");

    if (archivos.isEmpty() || archivos.size() > 5) {
      throw new BadRequestResponse(
          "Adjuntá entre una y cinco fotos."
      );
    }

    List<Foto> fotos = new ArrayList<>();

    // Validamos todas antes de escribir.
    for (var archivo : archivos) {
      byte[] bytes;

      try (var input = archivo.content()) {
        bytes = input.readNBytes(MAX + 1);
      }

      if (bytes.length == 0 || bytes.length > MAX) {
        throw new BadRequestResponse(
            "Cada foto debe pesar como máximo 5 MB."
        );
      }

      String extension = validarImagen(bytes);

      try {
        String hash = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(bytes)
        );

        fotos.add(new Foto(hash + "." + extension, bytes));
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException(e);
      }
    }

    Path destino = carpeta.resolve(id);
    Files.createDirectories(destino);

    List<String> referencias = new ArrayList<>();

    for (Foto foto : fotos) {
      Path archivo = destino.resolve(foto.nombre());

      // El mismo contenido conserva el mismo nombre en los reintentos.
      if (!Files.exists(archivo)) {
        Path temporal = Files.createTempFile(
            destino, "foto-", ".tmp"
        );

        try {
          Files.write(temporal, foto.bytes());

          try {
            Files.move(temporal, archivo);
          } catch (FileAlreadyExistsException ignored) {
          }
        } finally {
          Files.deleteIfExists(temporal);
        }
      }

      referencias.add(
          "/mi-entidad/fotos/" + id + "/" + foto.nombre()
      );
    }

    panel.llamarLogistica(
        "POST",
        "/rutas/id/"
            + UUID.fromString(seguimiento.path("idRuta").asText())
            + "/entregas/" + id + "/recepcion",
        Map.of(
            "fotos",
            referencias.stream().distinct().toList()
        )
    );

    ctx.json(Map.of(
        "mensaje",
        "Recepción confirmada y fotos guardadas."
    ));
  }

  public void foto(Context ctx) throws IOException {
    String id = panel.donacionPropia(
        ctx.pathParam("id")
    ).getIdAsString();

    String nombre = ctx.pathParam("archivo");

    if (!nombre.matches("[a-f0-9]{64}\\.(jpg|png)")) {
      throw new NotFoundResponse();
    }

    Path archivo = carpeta.resolve(id).resolve(nombre);

    if (!Files.isRegularFile(archivo)) {
      throw new NotFoundResponse("No se encontró la foto.");
    }

    ctx.header("Cache-Control", "no-store");
    ctx.header("X-Content-Type-Options", "nosniff");

    ctx.contentType(
        nombre.endsWith(".png")
            ? "image/png"
            : "image/jpeg"
    );

    ctx.result(
        new ByteArrayInputStream(Files.readAllBytes(archivo))
    );
  }

  private static String validarImagen(byte[] bytes)
      throws IOException {

    try (var input = ImageIO.createImageInputStream(
        new ByteArrayInputStream(bytes)
    )) {
      var readers = ImageIO.getImageReaders(input);

      if (!readers.hasNext()) {
        throw new BadRequestResponse(
            "Usá fotos JPG o PNG válidas."
        );
      }

      var reader = readers.next();

      try {
        reader.setInput(input);

        String formato = reader.getFormatName()
            .toLowerCase(Locale.ROOT);

        if (!List.of("jpeg", "jpg", "png").contains(formato)) {
          throw new BadRequestResponse("Usá fotos JPG o PNG.");
        }

        long pixeles =
            (long) reader.getWidth(0) * reader.getHeight(0);

        if (pixeles > 20_000_000L || pixeles < 1) {
          throw new BadRequestResponse(
              "La foto no puede superar 20 megapíxeles."
          );
        }

        if (reader.read(0) == null) {
          throw new BadRequestResponse("La foto está dañada.");
        }

        return formato.equals("png") ? "png" : "jpg";
      } finally {
        reader.dispose();
      }
    } catch (javax.imageio.IIOException e) {
      throw new BadRequestResponse(
          "La foto está dañada o su formato no es válido."
      );
    }
  }
}