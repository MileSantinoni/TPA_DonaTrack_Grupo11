package org.example.dominio.donacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.NecesidadExtraordinaria;
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

class PersistenciaAsignacionTest {

  private EntityManagerFactory factory;
  private EntityManager em;
  private RepositorioAsignacionesDonacion repositorio;

  @BeforeEach
  void preparar() {
    factory = Persistence.createEntityManagerFactory("donaciones-test");
    em = factory.createEntityManager();
    repositorio = new RepositorioAsignacionesDonacion(em);
  }

  private void abrirNuevoContexto() {
    em.close();
    em = factory.createEntityManager();
    repositorio = new RepositorioAsignacionesDonacion(em);
  }

  @Test
  void guardaRecuperaYBuscaAsignacion() {
    LocalDate fecha = LocalDate.of(2026, 9, 25);

    PersonaHumana donante = new PersonaHumana(
        "carlos@example.org", "11223344", TipoDocumento.DNI,
        "Carlos", "Gomez", 40, Genero.MASCULINO, "Calle 1"
    );

    Categoria categoria = new Categoria("Alimentos");
    Subcategoria subcategoria = new Subcategoria(
        "SECOS", "Alimentos secos", TipoAtributo.NO_PERECEDERO
    );
    categoria.agregarSubcategoria(subcategoria);

    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        "Comedor", "Calle 2", "12345678"
    );

    NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria(
        "Alimentos", 20, subcategoria, "Campania de invierno"
    );

    Donacion donacion = new Donacion(
        "Arroz", 5, "kg", subcategoria, null, null, donante
    );

    // Primero guardamos las entidades referenciadas.
    em.getTransaction().begin();
    em.persist(donante);
    em.persist(categoria);
    em.persist(entidad);
    em.persist(necesidad);
    em.persist(donacion);
    em.getTransaction().commit();

    String idEntidad = entidad.getIdAsString();
    String idDonacion = donacion.getIdAsString();

    AsignacionDonacion original = new AsignacionDonacion(
        donacion, entidad, necesidad, fecha
    );

    repositorio.agregar(original);

    String idAsignacion = original.getIdAsString();
    assertNotNull(idAsignacion);

    abrirNuevoContexto();

    AsignacionDonacion recuperada =
        repositorio.buscarPorId(idAsignacion).orElseThrow();

    assertNotSame(original, recuperada);
    assertEquals(idDonacion, recuperada.getDonacion().getIdAsString());
    assertEquals(idEntidad, recuperada.getEntidad().getIdAsString());
    assertEquals("Alimentos", recuperada.getNecesidad().getDescripcion());
    assertEquals(fecha, recuperada.getFechaRecepcion());

    assertEquals(
        List.of(idAsignacion),
        repositorio.buscarPorEntidad(idEntidad).stream()
            .map(AsignacionDonacion::getIdAsString)
            .toList()
    );

    // Ambos extremos del rango están incluidos.
    assertEquals(1, repositorio.buscarEntreFechas(fecha, fecha).size());
    assertTrue(
        repositorio.buscarEntreFechas(
            fecha.plusDays(1), fecha.plusDays(2)
        ).isEmpty()
    );

    assertTrue(repositorio.buscarPorId("invalido").isEmpty());
    assertTrue(repositorio.buscarPorEntidad("invalido").isEmpty());
    assertTrue(
        repositorio.buscarPorEntidad(UUID.randomUUID().toString()).isEmpty()
    );
    assertEquals(1, repositorio.buscarTodas().size());

    // Eliminar la asignación no debe eliminar la donación ni la entidad.
    repositorio.eliminar(recuperada);

    abrirNuevoContexto();

    assertTrue(repositorio.buscarPorId(idAsignacion).isEmpty());
    assertNotNull(em.find(Donacion.class, UUID.fromString(idDonacion)));
    assertNotNull(
        em.find(EntidadBeneficiaria.class, UUID.fromString(idEntidad))
    );
  }

  @Test
  void recuperaLasRelacionesDeLaEntidadBeneficiaria() {
    Categoria categoria = new Categoria("Ropa");

    Subcategoria subcategoria = new Subcategoria(
        "ABRIGO", "Abrigo", TipoAtributo.NO_PERECEDERO
    );
    categoria.agregarSubcategoria(subcategoria);

    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        "Hogar de prueba", "Calle 3", "12345678"
    );

    entidad.registrarNecesidad(new NecesidadExtraordinaria(
        "Camperas", 10, subcategoria, "Invierno"
    ));

    entidad.agregarRepresentante(
        new org.example.dominio.donante.Representante(
            "Ana", "Perez", "ana@example.org"
        )
    );

    em.getTransaction().begin();
    em.persist(categoria);
    em.persist(entidad);
    em.getTransaction().commit();

    UUID idEntidad = entidad.getId();

    abrirNuevoContexto();

    EntidadBeneficiaria recuperada =
        em.find(EntidadBeneficiaria.class, idEntidad);

    assertNotNull(recuperada);
    assertEquals(1, recuperada.getNecesidades().size());
    assertEquals(
        "Camperas",
        recuperada.getNecesidades().get(0).getDescripcion()
    );
    assertNotNull(recuperada.getNecesidades().get(0).getId());

    assertEquals(1, recuperada.getRepresentantes().size());
    assertEquals(
        "ana@example.org",
        recuperada.getRepresentantes().get(0).getEmail()
    );
    assertNotNull(recuperada.getRepresentantes().get(0).getId());
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