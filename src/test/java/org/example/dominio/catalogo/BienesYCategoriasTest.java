package org.example.dominio.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    categoriaAlimentos = new Categoria("Alimentos");
    subcategoriaFideos = new Subcategoria("fideos secos");
    categoriaAlimentos.agregarSubcategoria(subcategoriaFideos);

    categoriaMobiliario = new Categoria("Mobiliario");
    subcategoriaSillas = new Subcategoria("sillas");
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

    BienPerecedero fideos = new BienPerecedero(
        "Paquetes de fideos secos",
        100,
        "unidades",
        subcategoriaFideos,
        fechaVencimiento
    );

    // atributos heredados de la clase abstracta Bien
    assertEquals("Paquetes de fideos secos", fideos.getDescripcion());
    assertEquals(100, fideos.getCantidad());
    assertEquals("unidades", fideos.getUnidadMedida());
    assertEquals(subcategoriaFideos, fideos.getSubcategoria());

    // Verificamos fecha de venc
    assertEquals(fechaVencimiento, fideos.getFechaVencimiento());
  }

  @Test
  public void testCreacionBienConEstado() {
    // seis sillas usadas
    BienConEstado sillas = new BienConEstado(
        "Sillas de oficina corporativa",
        6,
        "unidades",
        subcategoriaSillas,
        Estado.USADO
    );

    // Verificamos los atributos heredados de la clase abstracta Bien
    assertEquals("Sillas de oficina corporativa", sillas.getDescripcion());
    assertEquals(6, sillas.getCantidad());
    assertEquals(subcategoriaSillas, sillas.getSubcategoria());

    // atributo específico del Bien Con Estado
    assertEquals(Estado.USADO, sillas.getEstado());
  }
}