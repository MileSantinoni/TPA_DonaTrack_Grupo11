package org.example.dominio.donacion;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.Repositorios.RepositorioRegistroDonacion;
import org.example.dominio.catalogo.Bien;
import org.example.dominio.catalogo.Categoria;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RepositorioRegistroDonacionTest {

  private EntityManagerFactory factory;
  private EntityManager em;
  private RepositorioRegistroDonacion repositorio;
  private PersonaHumana donante;
  private Subcategoria subcategoria;

  @BeforeEach
  void preparar() {
    factory = Persistence.createEntityManagerFactory("donaciones-test");
    em = factory.createEntityManager();
    repositorio = new RepositorioRegistroDonacion(em);

    donante = new PersonaHumana(
        "juan@example.org",
        "12345678",
        TipoDocumento.DNI,
        "Juan",
        "Perez",
        35,
        Genero.MASCULINO,
        "Calle 1"
    );

    Categoria categoria = new Categoria("Alimentos");

    subcategoria = new Subcategoria(
        "SECOS",
        "Alimentos secos",
        TipoAtributo.NO_PERECEDERO
    );

    categoria.agregarSubcategoria(subcategoria);

    em.getTransaction().begin();
    em.persist(donante);
    em.persist(categoria);
    em.getTransaction().commit();
  }

  private void abrirNuevoContexto() {
    em.close();
    em = factory.createEntityManager();
    repositorio = new RepositorioRegistroDonacion(em);
  }

  @Test
  void guardaYRecuperaRegistroConSusBienes() {
    Long idDonante = donante.getId();

    RegistroDonacion original = new RegistroDonacion(
        "REG-001",
        "Alimentos para el comedor",
        donante
    );

    Bien arroz = new Bien(
        "BIEN-001",
        "Arroz",
        10,
        "kg",
        subcategoria,
        null,
        null
    );
    arroz.setFoto("arroz.jpg");

    original.agregarBien(arroz);
    repositorio.agregarRegistro(original);

    abrirNuevoContexto();

    RegistroDonacion recuperado = repositorio.buscarPorId("REG-001");

    assertNotNull(recuperado);
    assertNotSame(original, recuperado);
    assertEquals(
        "Alimentos para el comedor",
        recuperado.getDescripcionGeneral()
    );
    assertEquals(idDonante, recuperado.getDonante().getId());
    assertEquals(original.getFechaDeRegistro(), recuperado.getFechaDeRegistro());

    assertEquals(1, recuperado.getListaBienes().size());

    Bien bien = recuperado.getListaBienes().get(0);

    assertEquals("BIEN-001", bien.getId());
    assertEquals("Arroz", bien.getDescripcion());
    assertEquals(10, bien.getCantidad());
    assertEquals("kg", bien.getUnidadMedida());
    assertEquals("arroz.jpg", bien.getFoto());
    assertEquals("Alimentos secos", bien.getSubcategoria().getNombre());

    // La segmentación debe seguir funcionando después de recuperar.
    var donaciones = recuperado.segmentar();

    assertEquals(1, donaciones.size());

    Donacion segmentada = donaciones.get(0);

    assertEquals("Arroz", segmentada.getDescripcionGeneral());
    assertEquals(10, segmentada.getCantidad());
    assertEquals(idDonante, segmentada.getDonante().getId());
    assertEquals(
        recuperado.getFechaDeRegistro(),
        segmentada.getFechaDeRegistro()
    );
    assertEquals(EstadoDonacion.EN_DEPOSITO, segmentada.getEstadoActual());
  }

  @Test
  void buscaPorIdYListaLosRegistrosPersistidos() {
    repositorio.agregarRegistro(
        new RegistroDonacion("REG-001", "Primera entrega", donante)
    );
    repositorio.agregarRegistro(
        new RegistroDonacion("REG-002", "Segunda entrega", donante)
    );

    abrirNuevoContexto();

    RegistroDonacion segundo = repositorio.buscarPorId("REG-002");

    assertNotNull(segundo);
    assertEquals("Segunda entrega", segundo.getDescripcionGeneral());
    assertEquals(2, repositorio.obtenerTodos().size());
    assertNull(repositorio.buscarPorId("INEXISTENTE"));
  }

  @Test
  void respetaElRollbackDeLaTransaccionExterna() {
    RegistroDonacion registro = new RegistroDonacion(
        "REG-ROLLBACK", "No debe quedar", donante
    );

    registro.agregarBien(new Bien(
        "BIEN-ROLLBACK", "Fideos", 5, "kg",
        subcategoria, null, null
    ));

    var generadas = registro.segmentar();

    em.getTransaction().begin();

    repositorio.agregarRegistroConDonaciones(registro, generadas);

    // Ejecutamos las escrituras sin confirmar la transacción.
    em.flush();

    String idDonacion = generadas.get(0).getIdAsString();
    assertNotNull(idDonacion);

    em.getTransaction().rollback();

    abrirNuevoContexto();

    assertNull(repositorio.buscarPorId("REG-ROLLBACK"));
    assertNull(em.find(Bien.class, "BIEN-ROLLBACK"));
    assertNull(em.find(
        Donacion.class,
        java.util.UUID.fromString(idDonacion)
    ));
  }

  @Test
  void guardaRegistroBienYDonacionEnLaMismaOperacion() {
    RegistroDonacion registro = new RegistroDonacion(
        "REG-CONJUNTO", "Entrega de alimentos", donante
    );

    registro.agregarBien(new Bien(
        "BIEN-CONJUNTO", "Arroz", 10, "kg",
        subcategoria, null, null
    ));

    var generadas = registro.segmentar();

    repositorio.agregarRegistroConDonaciones(registro, generadas);

    String idDonacion = generadas.get(0).getIdAsString();
    assertNotNull(idDonacion);

    abrirNuevoContexto();

    RegistroDonacion recuperado =
        repositorio.buscarPorId("REG-CONJUNTO");

    assertNotNull(recuperado);
    assertEquals(1, recuperado.getListaBienes().size());
    assertNotNull(em.find(Bien.class, "BIEN-CONJUNTO"));

    Donacion donacion = em.find(
        Donacion.class,
        java.util.UUID.fromString(idDonacion)
    );

    assertNotNull(donacion);
    assertEquals("Arroz", donacion.getDescripcionGeneral());
    assertEquals(10, donacion.getCantidad());
    assertEquals(
        recuperado.getDonante().getId(),
        donacion.getDonante().getId()
    );
    assertEquals(
        recuperado.getFechaDeRegistro(),
        donacion.getFechaDeRegistro()
    );
    assertEquals(EstadoDonacion.EN_DEPOSITO, donacion.getEstadoActual());
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