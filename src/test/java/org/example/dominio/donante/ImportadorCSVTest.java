package org.example.dominio.donante;

import org.example.Repositorios.RepositorioDonantes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ImportadorCSVTest {

  @BeforeEach
  void setUp() {
    RepositorioDonantes.getInstance().limpiar();
  }

  @Test
  void importaDonantesDesdeCSV() throws IOException {
    RepositorioDonantes.getInstance().limpiar();
    ImportadorCSV importador = new ImportadorCSV();

    importador.importar("src/test/resources/donantes_import_20000_UTF8_BOM.csv");

    assertEquals(importador.getDonantesCreados(), importador.getDonantesCreados());
    assertEquals(14, importador.getDonantesActualizados());
  }

  @Test
  void actualizaLaInformacionDelDonanteExistente() throws IOException {

    RepositorioDonantes.getInstance().limpiar();

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

    RepositorioDonantes.getInstance().agregar(anaVieja);

    ImportadorCSV importador = new ImportadorCSV();

    importador.importar(
        "src/test/resources/donantes_import_20000_UTF8_BOM.csv"
    );

    assertEquals(19986, RepositorioDonantes.getInstance().buscarTodos().size());
    assertEquals("28456905", anaVieja.getNumeroDocumento());
    assertEquals(1, anaVieja.getMediosDeContacto().size());
    assertEquals("+54 11 5181-9600", anaVieja.getMediosDeContacto().get(0).getNumero());
  }
}