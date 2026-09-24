package org.example.dominio.donacion;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PersistenciaDonacionTest implements WithSimplePersistenceUnit {

  @Test
  public void testPersistirDonacionYDonante() {
    withTransaction(() -> {
      PersonaHumana donante = new PersonaHumana(
          "ana@mail.com", "87654321", TipoDocumento.DNI,
          "Ana", "Perez", 30, Genero.FEMENINO, "Av. Corrientes 1500"
      );

      Subcategoria fideos = new Subcategoria("SUB-3", "Fideos", TipoAtributo.NO_PERECEDERO);

      Donacion donacion = new Donacion(
          "Paquetes de fideos", 100, "PAQUETE",
          fideos, null, null, null, donante
      );

      entityManager().persist(donante);
      entityManager().persist(fideos);
      entityManager().persist(donacion);

      assertNotNull(donacion.getIdAsString());
      assertEquals(EstadoDonacion.EN_DEPOSITO, donacion.getEstadoActual());
    });
  }
}
