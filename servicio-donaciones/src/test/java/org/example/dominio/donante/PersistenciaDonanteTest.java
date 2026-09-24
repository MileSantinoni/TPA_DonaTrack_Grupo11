package org.example.dominio.donante;

import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PersistenciaDonanteTest implements WithSimplePersistenceUnit {

  @Test
  public void testPersistirPersonaHumanaConMedioContacto() {
    withTransaction(() -> {
      PersonaHumana donante = new PersonaHumana(
          "pedro@mail.com",
          "12345678",
          TipoDocumento.DNI,
          "Pedro",
          "Gonzales",
          25,
          Genero.MASCULINO,
          "Av. Libertador 1234"
      );

      MedioContacto whatsapp = new MedioContacto(TipoMedioContacto.WHATSAPP, "1166778899");
      donante.agregarMedioContacto(whatsapp);

      entityManager().persist(donante);

      assertNotNull(donante.getId(), "El donante humano debe tener un ID asignado");
      assertEquals(1, donante.getMediosDeContacto().size());
    });
  }
}
