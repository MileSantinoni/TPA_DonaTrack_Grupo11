package org.example.dominio.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class BienesYCategoriasTest {

  private Categoria categoriaAlimentos;
  private Categoria categoriaMobiliario;
  private Subcategoria subcategoriaFideos;
  private Subcategoria subcategoriaSillas;

  @BeforeEach
  public void setUp() {
    categoriaAlimentos = new Categoria( "Alimentos");

    // CORRECTO: Le pasamos TipoAtributo.PERECEDERO
    subcategoriaFideos = new Subcategoria("SUB-1", "fideos secos", TipoAtributo.PERECEDERO);
    categoriaAlimentos.agregarSubcategoria(subcategoriaFideos);

    categoriaMobiliario = new Categoria("Mobiliario");

    // CORRECTO: Le pasamos TipoAtributo.CON_ESTADO
    subcategoriaSillas = new Subcategoria("SUB-2", "sillas", TipoAtributo.CON_ESTADO);
    categoriaMobiliario.agregarSubcategoria(subcategoriaSillas);
  }

  @Test
  public void testCreacionCategoriaYSubcategorias() {
    // agrupamos subcategorias
    assertEquals("Alimentos", categoriaAlimentos.getNombre());
    assertEquals(1, categoriaAlimentos.getSubcategorias().size());
    assertEquals("fideos secos", categoriaAlimentos.getSubcategorias().get(0).getNombre());
  }

  @Test
  public void testCreacionBienPerecedero() {
    // 100 paquetes de fideos que vencen el 01/01/2027
    LocalDate fechaVencimiento = LocalDate.of(2027, 1, 1);

    Bien fideos = new Bien(
        "B-001",
        "Paquetes de fideos secos",
        100,
        "unidades",
        subcategoriaFideos,
        fechaVencimiento,
        null
    );

    assertEquals("Paquetes de fideos secos", fideos.getDescripcion());
    assertEquals(100, fideos.getCantidad());
    assertEquals("unidades", fideos.getUnidadMedida());
    assertEquals(subcategoriaFideos, fideos.getSubcategoria());

    assertEquals(fechaVencimiento, fideos.getFechaVencimiento());
    assertNull(fideos.getEstado(), "Un bien perecedero no debería tener CondicionBien");
  }

  @Test
  public void testCreacionBienConEstado() {
    // seis sillas usadas
    Bien sillas = new Bien(
        "B-002",
        "Sillas de oficina corporativa",
        6,
        "unidades",
        subcategoriaSillas,
        null,
        Estado.USADO
    );

    assertEquals("Sillas de oficina corporativa", sillas.getDescripcion());
    assertEquals(6, sillas.getCantidad());
    assertEquals(subcategoriaSillas, sillas.getSubcategoria());

    assertEquals(Estado.USADO, sillas.getEstado());
    assertNull(sillas.getFechaVencimiento(), "Un bien mobiliario no debería tener fecha de vencimiento");
  }
}