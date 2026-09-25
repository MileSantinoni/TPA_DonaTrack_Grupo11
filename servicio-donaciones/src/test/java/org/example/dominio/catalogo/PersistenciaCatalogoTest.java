package org.example.dominio.catalogo;

import java.time.LocalDate;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.Repositorios.RepositorioCatalogo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PersistenciaCatalogoTest {

  private EntityManagerFactory factory;
  private EntityManager em;
  private RepositorioCatalogo catalogo;

  @BeforeEach
  void preparar() {
    factory = Persistence.createEntityManagerFactory("catalogo-test");
    em = factory.createEntityManager();
    catalogo = new RepositorioCatalogo(em);
  }

  private void abrirOtraConexion() {
    em.close();
    em = factory.createEntityManager();
    catalogo = new RepositorioCatalogo(em);
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
  void recuperaCategoriaSubcategoriaYBienPerecedero() {
    Categoria alimentos = new Categoria("Alimentos");
    Subcategoria lacteos = new Subcategoria(
        "SUB-LACTEOS", "Lacteos", TipoAtributo.PERECEDERO
    );
    alimentos.agregarSubcategoria(lacteos);

    LocalDate vencimiento = LocalDate.of(2030, 1, 15);
    Bien leche = new Bien(
        "BIEN-LECHE", "Leche", 12, "litros",
        lacteos, vencimiento, null
    );
    leche.setFoto("leche.jpg");

    catalogo.agregarCategoria(alimentos);
    catalogo.agregarBien(leche);

    Long idCategoria = alimentos.getId();
    assertNotNull(idCategoria);

    abrirOtraConexion();

    Categoria recuperada =
        catalogo.buscarCategoria(idCategoria).orElseThrow();

    assertNotSame(alimentos, recuperada);
    assertEquals("Alimentos", recuperada.getNombre());
    assertEquals(1, recuperada.getSubcategorias().size());
    assertEquals(
        "SUB-LACTEOS",
        recuperada.getSubcategorias().get(0).getId()
    );

    Subcategoria subcategoria =
        catalogo.buscarSubcategoria("SUB-LACTEOS").orElseThrow();

    assertEquals(idCategoria, subcategoria.getCategoria().getId());
    assertEquals(TipoAtributo.PERECEDERO, subcategoria.getTipo());

    Bien bien = catalogo.buscarBien("BIEN-LECHE").orElseThrow();

    assertEquals("Leche", bien.getDescripcion());
    assertEquals(12, bien.getCantidad());
    assertEquals("litros", bien.getUnidadMedida());
    assertEquals(vencimiento, bien.getFechaVencimiento());
    assertEquals("leche.jpg", bien.getFoto());
    assertEquals("SUB-LACTEOS", bien.getSubcategoria().getId());
  }

  @Test
  void conservaEstadoDelBienYPermiteActualizarlo() {
    Categoria ropa = new Categoria("Ropa");
    Subcategoria abrigos = new Subcategoria(
        "SUB-ABRIGOS", "Abrigos", TipoAtributo.CON_ESTADO
    );
    ropa.agregarSubcategoria(abrigos);

    Bien bien = new Bien(
        "BIEN-ABRIGOS", "Camperas", 3, "unidades",
        abrigos, null, Estado.USADO
    );

    catalogo.agregarCategoria(ropa);
    catalogo.agregarBien(bien);

    abrirOtraConexion();

    Bien recuperado = catalogo.buscarBien("BIEN-ABRIGOS").orElseThrow();
    assertEquals(Estado.USADO, recuperado.getEstado());

    recuperado.setCantidad(5);
    recuperado.setDescripcion("Camperas de invierno");
    catalogo.actualizarBien(recuperado);

    abrirOtraConexion();

    Bien actualizado = catalogo.buscarBien("BIEN-ABRIGOS").orElseThrow();
    assertEquals(5, actualizado.getCantidad());
    assertEquals("Camperas de invierno", actualizado.getDescripcion());
    assertEquals(Estado.USADO, actualizado.getEstado());
  }

  @Test
  void cambiarCategoriaActualizaAmbosLadosYLaRelacionPersistida() {
    Categoria origen = new Categoria("Origen");
    Categoria destino = new Categoria("Destino");
    Subcategoria subcategoria = new Subcategoria(
        "SUB-MOVER", "Conservas", TipoAtributo.NO_PERECEDERO
    );

    origen.agregarSubcategoria(subcategoria);
    catalogo.agregarCategoria(origen);
    catalogo.agregarCategoria(destino);

    Long idOrigen = origen.getId();
    Long idDestino = destino.getId();

    abrirOtraConexion();

    Categoria anterior = catalogo.buscarCategoria(idOrigen).orElseThrow();
    Categoria nueva = catalogo.buscarCategoria(idDestino).orElseThrow();
    Subcategoria mover = catalogo.buscarSubcategoria("SUB-MOVER").orElseThrow();

    nueva.agregarSubcategoria(mover);

    assertTrue(anterior.getSubcategorias().isEmpty());
    assertEquals(1, nueva.getSubcategorias().size());

    catalogo.actualizarCategoria(nueva);

    abrirOtraConexion();

    assertTrue(
        catalogo.buscarCategoria(idOrigen).orElseThrow()
            .getSubcategorias().isEmpty()
    );
    assertEquals(
        idDestino,
        catalogo.buscarSubcategoria("SUB-MOVER").orElseThrow()
            .getCategoria().getId()
    );
  }
}