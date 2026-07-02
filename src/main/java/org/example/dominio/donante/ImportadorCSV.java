package org.example.dominio.donante;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ImportadorCSV {

  private List<Donante> donantes;
  private int donantesCreados;
  private int donantesActualizados;

  public ImportadorCSV(List<Donante> donantes) {
    this.donantes = donantes;
    this.donantesCreados = 0;
    this.donantesActualizados = 0;
  }

  public void importar(String rutaArchivo) throws IOException {

    Reader reader = new FileReader(rutaArchivo, StandardCharsets.UTF_8);

    CSVParser parser = CSVFormat.DEFAULT
        .builder()
        .setHeader("TipoPersona", "TipoDoc", "Documento", "Nombre/Razón Social", "Email", "Teléfono")
        .setSkipHeaderRecord(true)
        .setIgnoreHeaderCase(true)
        .setTrim(true)
        .build()
        .parse(reader);

    for (CSVRecord fila : parser) {

      String tipoPersona = fila.get("TipoPersona");
      TipoDocumento tipoDocumento = TipoDocumento.valueOf(fila.get("TipoDoc"));
      String documento = fila.get("Documento");
      String nombre = fila.get("Nombre/Razón Social");
      String email = fila.get("Email");
      String telefono = fila.get("Teléfono");

      Donante existente = buscarPorMail(email);

      if (existente != null) {
        existente.actualizarDatos(email, documento, tipoDocumento);
        existente.agregarMedioContacto(new MedioContacto(TipoMedioContacto.TELEFONO, telefono));
        donantesActualizados++;
      } else {
        Donante nuevoDonante;

        if (tipoPersona.equals("HUMANA")) {
          nuevoDonante = new PersonaHumana(
              email,
              documento,
              tipoDocumento,
              nombre,
              "",
              0,
              Genero.OTRO,
              ""
          );
        } else {
          nuevoDonante = new PersonaJuridica(
              email,
              documento,
              tipoDocumento,
              nombre,
              TipoOrganizacion.EMPRESA,
              ""
          );
        }

        nuevoDonante.agregarMedioContacto(
            new MedioContacto(TipoMedioContacto.TELEFONO, telefono)
        );

        donantes.add(nuevoDonante);
        donantesCreados++;
      }
    }

    parser.close();
    reader.close();
  }

  private Donante buscarPorMail(String email) {
    return donantes.stream()
        .filter(d -> d.getMail().equals(email))
        .findFirst()
        .orElse(null);
  }

  public int getDonantesCreados() {
    return donantesCreados;
  }

  public int getDonantesActualizados() {
    return donantesActualizados;
  }
}