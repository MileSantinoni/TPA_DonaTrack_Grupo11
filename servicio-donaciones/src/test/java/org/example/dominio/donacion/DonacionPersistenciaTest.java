package org.example.dominio.donacion;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.Repositorios.RepositorioDonaciones;
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

class DonacionPersistenciaTest {

  private EntityManagerFactory factory;
  private EntityManager em;
  private RepositorioDonaciones repositorio;

  @BeforeEach
  void preparar() {
    factory = Persistence.createEntityManagerFactory("donaciones-test");
    em = factory.createEntityManager();
    repositorio = new RepositorioDonaciones(em);
  }

  private void abrirNuevoContexto() {
    em.close();
    em = factory.createEntityManager();
    repositorio = new RepositorioDonaciones(em);
  }

  @Test
  void conservaEstadoHistorialYReglasAlRecuperar() {
    PersonaHumana donante = new PersonaHumana(
        "ana@example.org",
        "12345678",
        TipoDocumento.DNI,
        "Ana",
        "Perez",
        30,
        Genero.FEMENINO,
        "Calle 1"
    );

    Categoria categoria = new Categoria("Alimentos");
    Subcategoria subcategoria = new Subcategoria(
        "ALIMENTOS-SECOS",
        "Alimentos secos",
        TipoAtributo.NO_PERECEDERO
    );
    categoria.agregarSubcategoria(subcategoria);

    em.getTransaction().begin();
    em.persist(donante);
    em.persist(categoria);
    em.getTransaction().commit();

    Long idDonante = donante.getId();

    Donacion original = new Donacion(
        "Arroz", 10, "kg", subcategoria, null, null, donante
    );

    repositorio.agregar(original);

    String id = original.getIdAsString();
    assertNotNull(id);

    abrirNuevoContexto();

    Donacion recuperada = repositorio.buscarPorId(id).orElseThrow();

    assertNotSame(original, recuperada);
    assertEquals("Arroz", recuperada.getDescripcionGeneral());
    assertEquals(10, recuperada.getCantidad());
    assertEquals(idDonante, recuperada.getDonante().getId());
    assertEquals("Alimentos secos", recuperada.getSubcategoria().getNombre());
    assertEquals(EstadoDonacion.EN_DEPOSITO, recuperada.getEstadoActual());
    assertTrue(recuperada.getHistorialEstados().isEmpty());

    // Una transición inválida debe seguir siendo rechazada tras cargar.
    assertThrows(
        IllegalStateException.class,
        () -> recuperada.cambiarEstado(
            EstadoDonacion.ENTREGADA, "Salto de estados"
        )
    );
    assertEquals(EstadoDonacion.EN_DEPOSITO, recuperada.getEstadoActual());
    assertTrue(recuperada.getHistorialEstados().isEmpty());

    recuperada.cambiarEstado(
        EstadoDonacion.ASIGNACION_REALIZADA,
        "Asignada al comedor"
    );
    repositorio.actualizar(recuperada);

    abrirNuevoContexto();

    Donacion asignada = repositorio.buscarPorId(id).orElseThrow();

    assertEquals(
        EstadoDonacion.ASIGNACION_REALIZADA,
        asignada.getEstadoActual()
    );
    assertEquals(1, asignada.getHistorialEstados().size());

    RegistroCambioEstado cambio = asignada.getHistorialEstados().get(0);

    assertEquals(EstadoDonacion.EN_DEPOSITO, cambio.getEstadoAnterior());
    assertEquals(
        EstadoDonacion.ASIGNACION_REALIZADA,
        cambio.getEstadoNuevo()
    );
    assertEquals("Asignada al comedor", cambio.getJustificativo());
    assertNotNull(cambio.getFechaYHora());

    // La reconstrucción del State también debe impedir repetir el estado.
    assertThrows(
        IllegalStateException.class,
        () -> asignada.cambiarEstado(
            EstadoDonacion.ASIGNACION_REALIZADA, "Repetida"
        )
    );
    assertEquals(1, asignada.getHistorialEstados().size());

    assertEquals(
        List.of(id),
        repositorio.buscarTodas().stream()
            .map(Donacion::getIdAsString)
            .toList()
    );
    assertTrue(repositorio.buscarPorId("id-invalido").isEmpty());
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