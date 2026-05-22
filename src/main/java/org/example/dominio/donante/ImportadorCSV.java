package org.example.dominio.donante;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ImportadorCSV {

  private List<Donante> donantes;
  //solo para fines de testear
  private int donantesCreados;
  private int donantesActualizados;

  public ImportadorCSV(List<Donante> donantes) {
    this.donantes = donantes;
    //solo para fines de testear
    this.donantesCreados = 0;
    this.donantesActualizados = 0;
  }

  public void importar(String rutaArchivo) throws IOException {

    BufferedReader reader = new BufferedReader(
        new InputStreamReader(
            new FileInputStream(rutaArchivo),
            StandardCharsets.UTF_8
        )
    );

    String linea;
    reader.readLine();

    while ((linea = reader.readLine()) != null) {

      String[] datos = linea.split(",");

      String tipoPersona = datos[0].replace("\uFEFF", "");
      TipoDocumento tipoDocumento = TipoDocumento.valueOf(datos[1]);
      String documento = datos[2];
      String nombre = datos[3];
      String email = datos[4];
      String telefono = datos[5];

      Donante existente = buscarPorMail(email);

      if (existente != null) {
        existente.actualizarDatos(email, documento, tipoDocumento);
        existente.agregarMedioContacto(new MedioContacto(TipoMedioContacto.TELEFONO, telefono));
        donantesActualizados++; //solo para fines de testear
      } else {
        Donante nuevoDonante;
        if (tipoPersona.equals("HUMANA")) {
          nuevoDonante = new PersonaHumana(email, documento, tipoDocumento, nombre, "", 0, Genero.OTRO, "");
        } else {
          nuevoDonante = new PersonaJuridica(email, documento, tipoDocumento, nombre, TipoOrganizacion.EMPRESA, "");
        }
        nuevoDonante.agregarMedioContacto(new MedioContacto(TipoMedioContacto.TELEFONO, telefono));
        donantes.add(nuevoDonante);
        donantesCreados++; //solo para fines de testear
      }
    }
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