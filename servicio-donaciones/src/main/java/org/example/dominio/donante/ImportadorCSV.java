package org.example.dominio.donante;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.apache.commons.csv.*;
import org.example.Repositorios.RepositorioDonantes;

public class ImportadorCSV {

  private static final List<String> COLUMNAS = List.of(
      "TipoPersona",
      "TipoDoc",
      "Documento",
      "Nombre/Razón Social",
      "Email",
      "Teléfono"
  );

  private final RepositorioDonantes repositorio;
  private int donantesCreados;
  private int donantesActualizados;

  public ImportadorCSV() {
    this(RepositorioDonantes.getInstance());
  }

  public ImportadorCSV(RepositorioDonantes repositorio) {
    this.repositorio = Objects.requireNonNull(repositorio);
  }

  public void importar(String ruta) throws IOException {
    try (Reader reader = Files.newBufferedReader(
        Path.of(ruta),
        StandardCharsets.UTF_8
    )) {
      importar(reader);
    }
  }

  // El llamador abre y confirma la transacción completa.
  public void importar(Reader entrada) throws IOException {
    donantesCreados = 0;
    donantesActualizados = 0;

    BufferedReader reader = new BufferedReader(entrada);

    // Acepta archivos UTF-8 con o sin BOM.
    reader.mark(1);

    if (reader.read() != '\uFEFF') {
      reader.reset();
    }

    reader.mark(8192);

    String cabecera = reader.readLine();

    if (cabecera == null) {
      throw new IllegalArgumentException("El archivo está vacío.");
    }

    reader.reset();

    char separador = cabecera.contains(";") ? ';' : ',';

    try (CSVParser parser = CSVFormat.DEFAULT.builder()
        .setDelimiter(separador)
        .setHeader()
        .setSkipHeaderRecord(true)
        .setIgnoreHeaderCase(true)
        .setTrim(true)
        .build()
        .parse(reader)) {

      if (parser.getHeaderNames().size() != COLUMNAS.size()
          || COLUMNAS.stream().anyMatch(
          c -> !parser.getHeaderMap().containsKey(c)
      )) {
        throw new IllegalArgumentException(
            "Encabezados esperados: " + String.join(", ", COLUMNAS)
        );
      }

      for (CSVRecord fila : parser) {
        try {
          if (!fila.isConsistent()) {
            throw new IllegalArgumentException(
                "Cantidad de columnas incorrecta."
            );
          }

          String tipo = fila.get("TipoPersona")
              .toUpperCase(Locale.ROOT);

          if (!List.of("HUMANA", "JURIDICA").contains(tipo)) {
            throw new IllegalArgumentException(
                "TipoPersona debe ser HUMANA o JURIDICA."
            );
          }

          TipoDocumento tipoDoc = TipoDocumento.valueOf(
              fila.get("TipoDoc").toUpperCase(Locale.ROOT)
          );

          String documento = fila.get("Documento");
          String nombre = fila.get("Nombre/Razón Social");
          String mail = fila.get("Email").toLowerCase(Locale.ROOT);
          String telefono = fila.get("Teléfono");

          if (documento.isBlank()
              || nombre.isBlank()
              || !mail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException(
                "Revisá documento, nombre y email."
            );
          }

          if (tipo.equals("JURIDICA")
              && tipoDoc != TipoDocumento.CUIT) {
            throw new IllegalArgumentException(
                "La persona jurídica requiere CUIT."
            );
          }

          Donante d = repositorio.buscarPorMail(mail).orElse(null);
          boolean nuevo = d == null;

          if (nuevo) {
            d = tipo.equals("HUMANA")
                ? new PersonaHumana(
                mail, documento, tipoDoc,
                nombre, "", 0, Genero.OTRO, ""
            )
                : new PersonaJuridica(
                mail, documento, tipoDoc,
                nombre, TipoOrganizacion.EMPRESA, ""
            );
          } else {
            if (tipo.equals("HUMANA")
                != (d instanceof PersonaHumana)) {
              throw new IllegalArgumentException(
                  "El email ya pertenece a otro tipo de persona."
              );
            }

            // Conserva los datos personales que el CSV no desglosa.
            d.actualizarDatos(mail, documento, tipoDoc);
          }

          if (!telefono.isBlank()) {
            MedioContacto contacto = d.getMediosDeContacto().stream()
                .filter(m -> m.getTipo() == TipoMedioContacto.TELEFONO)
                .min(Comparator.comparing(MedioContacto::getId))
                .orElse(null);

            if (contacto == null) {
              d.agregarMedioContacto(
                  new MedioContacto(
                      TipoMedioContacto.TELEFONO,
                      telefono
                  )
              );
            } else {
              contacto.setNumero(telefono);
            }
          }

          if (nuevo) {
            repositorio.agregar(d);
            donantesCreados++;
          } else {
            repositorio.actualizar(d);
            donantesActualizados++;
          }

          if ((donantesCreados + donantesActualizados) % 250 == 0) {
            repositorio.sincronizarYLiberarContexto();
          }
        } catch (IllegalArgumentException e) {
          throw new IllegalArgumentException(
              "Registro " + fila.getRecordNumber() + ": " + e.getMessage(),
              e
          );
        }
      }

      if (donantesCreados + donantesActualizados == 0) {
        throw new IllegalArgumentException(
            "No hay filas para importar."
        );
      }
    }
  }

  public int getDonantesCreados() {
    return donantesCreados;
  }

  public int getDonantesActualizados() {
    return donantesActualizados;
  }
}