package org.example.dominio.catalogo;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PersistenciaCatalogoTest implements WithSimplePersistenceUnit {

  @Test
  public void testPersistirBienYCategoria() {
    withTransaction(() -> {
      Categoria alimentos = new Categoria("Alimentos");
      Subcategoria fideos = new Subcategoria("SUB-1", "Fideos Secos", TipoAtributo.PERECEDERO);
      alimentos.agregarSubcategoria(fideos);

      Bien bienFideos = new Bien("B-001", "Fideosarroz", 50, "unidades", fideos, LocalDate.now().plusMonths(6), null);

      entityManager().persist(alimentos);
      entityManager().persist(bienFideos);

      assertNotNull(bienFideos.getId());
    });
  }
}