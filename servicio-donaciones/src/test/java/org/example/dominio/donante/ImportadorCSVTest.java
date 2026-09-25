package org.example.dominio.donante;

import org.example.Repositorios.RepositorioDonantes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
public class ImportadorCSVTest {

  private EntityManagerFactory factory;
  private EntityManager em;
  private RepositorioDonantes repositorio;

  @BeforeEach
  void setUp() {
    factory = Persistence.createEntityManagerFactory("donantes-test");
    em = factory.createEntityManager();
    repositorio = new RepositorioDonantes(em);
  }

  @Test
  void importaDonantesDesdeCSV() throws IOException {
    ImportadorCSV importador = new ImportadorCSV(repositorio);

    em.getTransaction().begin();

    importador.importar(
        "src/test/resources/donantes_import_20000_UTF8_BOM.csv"
    );

    em.getTransaction().commit();
    em.clear();

    assertEquals(19986, importador.getDonantesCreados());
    assertEquals(14, importador.getDonantesActualizados());
    assertEquals(19986, repositorio.buscarTodos().size());
  }

  @Test
  void actualizaLaInformacionDelDonanteExistente() throws IOException {

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

    repositorio.agregar(anaVieja);
    Long idAnaOriginal = anaVieja.getId();
    em.clear();

    ImportadorCSV importador = new ImportadorCSV(repositorio);

    em.getTransaction().begin();

    importador.importar(
        "src/test/resources/donantes_import_20000_UTF8_BOM.csv"
    );

    em.getTransaction().commit();
    em.clear();

    PersonaHumana anaRecuperada = (PersonaHumana) repositorio
        .buscarPorMail("ananavarro3658@yahoo.com")
        .orElseThrow();

    assertEquals(idAnaOriginal, anaRecuperada.getId());
    assertEquals("Ana", anaRecuperada.getNombre());
    assertEquals("Navarro", anaRecuperada.getApellido());
    assertEquals(19985, importador.getDonantesCreados());
    assertEquals(15, importador.getDonantesActualizados());
  }

  @AfterEach
  void cerrar() {
    try {
      if (em != null && em.isOpen()) {
        try {
          if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
          }
        } finally {
          em.close();
        }
      }
    } finally {
      if (factory != null && factory.isOpen()) {
        factory.close();
      }
    }
  }
}
