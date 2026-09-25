package org.example.dominio.donante;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.Repositorios.RepositorioDonantes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersistenciaDonanteTest {

  private EntityManagerFactory factory;
  private EntityManager em;
  private RepositorioDonantes repositorio;

  @BeforeEach
  void preparar() {
    factory = Persistence.createEntityManagerFactory("donantes-test");
    em = factory.createEntityManager();
    repositorio = new RepositorioDonantes(em);
  }

  private void abrirOtraConexion() {
    em.close();
    em = factory.createEntityManager();
    repositorio = new RepositorioDonantes(em);
  }

  private PersonaHumana nuevaPersonaHumana() {
    return new PersonaHumana(
        "ana@example.org",
        "12345678",
        TipoDocumento.DNI,
        "Ana",
        "Perez",
        30,
        Genero.FEMENINO,
        "Calle 123"
    );
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

  @Test
  void recuperaPersonaHumanaConSusContactos() {
    PersonaHumana persona = nuevaPersonaHumana();

    persona.agregarMedioContacto(
        new MedioContacto(TipoMedioContacto.WHATSAPP, "1122334455")
    );
    persona.activar();

    repositorio.agregar(persona);

    Long id = persona.getId();
    assertNotNull(id);

    abrirOtraConexion();

    Donante encontrado =
        repositorio.buscarPorId(id.toString()).orElseThrow();

    assertInstanceOf(PersonaHumana.class, encontrado);
    assertNotSame(persona, encontrado);

    PersonaHumana recuperada = (PersonaHumana) encontrado;

    assertEquals("Ana", recuperada.getNombre());
    assertEquals("Perez", recuperada.getApellido());
    assertEquals(30, recuperada.getEdad());
    assertEquals(Genero.FEMENINO, recuperada.getGenero());
    assertEquals("Calle 123", recuperada.getDireccion());
    assertEquals("12345678", recuperada.getNumeroDocumento());
    assertEquals(TipoDocumento.DNI, recuperada.getTipoDeDocumento());
    assertEquals(EstadoRegistro.ACTIVO, recuperada.getEstadoRegistro());
    assertEquals(
        TipoContactoPredeterminado.MAIL,
        recuperada.getContactoPredeterminado()
    );

    assertEquals(1, recuperada.getMediosDeContacto().size());

    MedioContacto contacto = recuperada.getMediosDeContacto().get(0);
    assertNotNull(contacto.getId());
    assertEquals(TipoMedioContacto.WHATSAPP, contacto.getTipo());
    assertEquals("1122334455", contacto.getNumero());

    assertEquals(
        id,
        repositorio.buscarPorMail("ana@example.org")
            .orElseThrow().getId()
    );
    assertTrue(repositorio.existeMail("ana@example.org"));
    assertTrue(repositorio.buscarPorId("no-es-un-numero").isEmpty());
  }

  @Test
  void recuperaPersonaJuridicaConSusRepresentantes() {
    PersonaJuridica persona = new PersonaJuridica(
        "contacto@example.org",
        "30123456789",
        TipoDocumento.CUIT,
        "Empresa Solidaria",
        TipoOrganizacion.EMPRESA,
        "Alimentos"
    );

    persona.agregarRepresentante(
        new Representante("Juan", "Lopez", "juan@example.org")
    );

    repositorio.agregar(persona);
    Long id = persona.getId();

    abrirOtraConexion();

    Donante encontrado =
        repositorio.buscarPorId(id.toString()).orElseThrow();

    assertInstanceOf(PersonaJuridica.class, encontrado);

    PersonaJuridica recuperada = (PersonaJuridica) encontrado;

    assertEquals("Empresa Solidaria", recuperada.getRazonSocial());
    assertEquals(TipoOrganizacion.EMPRESA, recuperada.getTipo());
    assertEquals("Alimentos", recuperada.getRubro());
    assertEquals(1, recuperada.getRepresentantes().size());

    Representante representante = recuperada.getRepresentantes().get(0);

    assertNotNull(representante.getId());
    assertEquals("Juan", representante.getNombre());
    assertEquals("Lopez", representante.getApellido());
    assertEquals("juan@example.org", representante.getEmail());
  }

  @Test
  void actualizaDatosYLosRecuperaDesdeLaBase() {
    PersonaHumana persona = nuevaPersonaHumana();
    repositorio.agregar(persona);
    String id = persona.getId().toString();

    abrirOtraConexion();

    PersonaHumana recuperada =
        (PersonaHumana) repositorio.buscarPorId(id).orElseThrow();

    recuperada.actualizarDatos(
        "nuevo@example.org", "87654321", TipoDocumento.DNI
    );
    recuperada.setDireccion("Otra calle 456");

    repositorio.actualizar(recuperada);

    abrirOtraConexion();

    PersonaHumana actualizada =
        (PersonaHumana) repositorio.buscarPorId(id).orElseThrow();

    assertEquals("nuevo@example.org", actualizada.getMail());
    assertEquals("87654321", actualizada.getNumeroDocumento());
    assertEquals("Otra calle 456", actualizada.getDireccion());

    assertFalse(repositorio.existeMail("ana@example.org"));
    assertTrue(repositorio.existeMail("nuevo@example.org"));
  }

  @Test
  void eliminaPersonaJuridicaConContactosYRepresentantes() {
    PersonaJuridica persona = new PersonaJuridica(
        "baja@example.org",
        "30987654321",
        TipoDocumento.CUIT,
        "Empresa de prueba",
        TipoOrganizacion.EMPRESA,
        "Textil"
    );

    persona.agregarMedioContacto(
        new MedioContacto(TipoMedioContacto.WHATSAPP, "1199998888")
    );
    persona.agregarRepresentante(
        new Representante("Maria", "Gomez", "maria@example.org")
    );

    repositorio.agregar(persona);
    String id = persona.getId().toString();

    abrirOtraConexion();

    Donante recuperado = repositorio.buscarPorId(id).orElseThrow();
    repositorio.eliminar(recuperado);

    abrirOtraConexion();

    assertTrue(repositorio.buscarPorId(id).isEmpty());
    assertTrue(repositorio.buscarTodos().isEmpty());

    assertEquals(
        0L,
        em.createQuery(
            "SELECT COUNT(m) FROM MedioContacto m", Long.class
        ).getSingleResult().longValue()
    );

    assertEquals(
        0L,
        em.createQuery(
            "SELECT COUNT(r) FROM RepresentanteDonante r", Long.class
        ).getSingleResult().longValue()
    );
  }
}