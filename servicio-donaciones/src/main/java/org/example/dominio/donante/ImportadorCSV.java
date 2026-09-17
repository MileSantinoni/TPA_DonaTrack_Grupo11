package org.example.dominio.donante;

import java.util.Optional;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.example.Repositorios.RepositorioDonantes;

public class ImportadorCSV {

  private RepositorioDonantes repositorio;
  private int donantesCreados;
  private int donantesActualizados;

  public ImportadorCSV() {
    this.repositorio = RepositorioDonantes.getInstance();
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

      Optional<Donante> existente = repositorio.buscarPorMail(email);

      if (existente.isPresent()) {
        Donante donante = existente.get();
        donante.actualizarDatos(email, documento, tipoDocumento);
        donante.agregarMedioContacto(new MedioContacto(TipoMedioContacto.TELEFONO, telefono));
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

        repositorio.agregar(nuevoDonante);
        donantesCreados++;
      }
    }

    parser.close();
    reader.close();
  }

  public int getDonantesCreados() {
    return donantesCreados;
  }

  public int getDonantesActualizados() {
    return donantesActualizados;
  }
}