package org.example.dominio.donante;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportadorCSVTest {

  @Test
  void importaDonantesDesdeCSV() throws IOException {
    List<Donante> donantes = new ArrayList<>();
    ImportadorCSV importador = new ImportadorCSV(donantes);

    importador.importar("src/test/resources/donantes_import_20000_UTF8_BOM.csv");

    assertEquals(donantes.size(), importador.getDonantesCreados());
    assertEquals(14, importador.getDonantesActualizados());
  }

  @Test
  void actualizaLaInformacionDelDonanteExistente() throws IOException {

    List<Donante> donantes = new ArrayList<>();

    PersonaHumana anaVieja = new PersonaHumana(
        "ananavarro3658@yahoo.com",
        "00000000",
        TipoDocumento.DNI,
        "Ana",
        "Navarro",
        30,
        Genero.FEMENINO,
        "Direccion vieja"
    );

    donantes.add(anaVieja);

    ImportadorCSV importador = new ImportadorCSV(donantes);

    importador.importar(
        "src/test/resources/donantes_import_20000_UTF8_BOM.csv"
    );

    assertTrue(donantes.size() <= 20000);
    assertEquals("28456905", anaVieja.getNumeroDocumento());
    assertEquals(1, anaVieja.getMediosDeContacto().size());
    assertEquals("+54 11 5181-9600", anaVieja.getMediosDeContacto().get(0).getNumero());
  }
}