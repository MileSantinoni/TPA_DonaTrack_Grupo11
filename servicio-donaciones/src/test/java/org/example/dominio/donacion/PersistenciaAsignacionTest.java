package org.example.dominio.donacion;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import java.time.LocalDate;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.NecesidadExtraordinaria;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PersistenciaAsignacionTest implements WithSimplePersistenceUnit {

  @Test
  public void testPersistirAsignacion() {
    withTransaction(() -> {
      EntidadBeneficiaria entidad = new EntidadBeneficiaria("hogar","Av Santa Fe","12345678");
      entityManager().persist(entidad);

      Subcategoria sub = new Subcategoria("SUB-ROPA", "Ropa", TipoAtributo.NO_PERECEDERO);
      entityManager().persist(sub);

      NecesidadExtraordinaria necesidad = new NecesidadExtraordinaria("Ropa de abrigo", 10, sub, "unMotivo");
      entityManager().persist(necesidad);

      PersonaHumana donante = new PersonaHumana("carlos@mail.com", "11223344", TipoDocumento.DNI, "Carlos", "Gomez", 40, Genero.MASCULINO, "Calle Falsa 123");
      entityManager().persist(donante);

      Donacion donacion = new Donacion("Camperas", 5, "UNIDAD", sub, null, null, null, donante);
      entityManager().persist(donacion);

      AsignacionDonacion asignacion = new AsignacionDonacion(donacion, entidad, necesidad, LocalDate.now());
      entityManager().persist(asignacion);

      assertNotNull(asignacion.getIdAsString(), "La asignación debe tener un ID generado");
    });
  }
}
