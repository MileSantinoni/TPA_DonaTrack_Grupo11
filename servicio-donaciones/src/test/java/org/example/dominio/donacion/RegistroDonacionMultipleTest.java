package org.example.dominio.donacion;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.Repositorios.RepositorioRegistroDonacion;
import org.example.dominio.catalogo.Bien;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistroDonacionMultipleTest {

  @Test
  void agrupaBienesCompatiblesYSeparaUnidades() {
    Subcategoria arroz = new Subcategoria(
        "ARROZ", "Arroz", TipoAtributo.NO_PERECEDERO
    );

    RegistroDonacion registro =
        new RegistroDonacion("REG-1", "Carga de alimentos", null);

    registro.agregarBien(bien(arroz, 10, "kg", null));
    registro.agregarBien(bien(arroz, 5, " KG ", null));
    registro.agregarBien(bien(arroz, 2, "UNIDAD", null));

    List<Donacion> donaciones = registro.segmentar();

    assertEquals(3, registro.getListaBienes().size());
    assertEquals(2, donaciones.size());
    assertEquals(15, donaciones.get(0).getCantidad());
    assertEquals(2, donaciones.get(1).getCantidad());

    assertTrue(donaciones.stream().allMatch(
        d -> d.getEstadoActual() == EstadoDonacion.EN_DEPOSITO
    ));
  }

  @Test
  void separaVencimientosDeLaMismaSubcategoria() {
    Subcategoria leche = new Subcategoria(
        "LECHE", "Leche", TipoAtributo.PERECEDERO
    );

    RegistroDonacion registro =
        new RegistroDonacion("REG-2", "Carga de leche", null);

    LocalDate primeraFecha = LocalDate.of(2027, 1, 10);
    LocalDate segundaFecha = LocalDate.of(2027, 2, 10);

    registro.agregarBien(bien(leche, 10, "LITRO", primeraFecha));
    registro.agregarBien(bien(leche, 5, "LITRO", segundaFecha));

    List<Donacion> donaciones = registro.segmentar();

    assertEquals(2, donaciones.size());
    assertEquals(primeraFecha, donaciones.get(0).getFechaVencimiento());
    assertEquals(segundaFecha, donaciones.get(1).getFechaVencimiento());
  }

  @Test
  void persisteUnaCargaConTresBienesYDosDonaciones() {
    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory(
            "donaciones-test",
            Map.of(
                "javax.persistence.jdbc.url",
                "jdbc:hsqldb:mem:carga_" + UUID.randomUUID(),
                "hibernate.hbm2ddl.auto", "create-drop"
            )
        );

    try {
      String idRegistro = UUID.randomUUID().toString();
      List<String> idsDonaciones;

      EntityManager em = factory.createEntityManager();

      try {
        em.getTransaction().begin();

        PersonaHumana donante = new PersonaHumana(
            "carga@example.org",
            "12345678",
            TipoDocumento.DNI,
            "Ana",
            "Perez",
            30,
            Genero.OTRO,
            "Calle 1"
        );

        Subcategoria arroz = new Subcategoria(
            "ARROZ", "Arroz", TipoAtributo.NO_PERECEDERO
        );

        Subcategoria fideos = new Subcategoria(
            "FIDEOS", "Fideos", TipoAtributo.NO_PERECEDERO
        );

        em.persist(donante);
        em.persist(arroz);
        em.persist(fideos);

        RegistroDonacion registro = new RegistroDonacion(
            idRegistro, "Carga completa", donante
        );

        registro.agregarBien(bien(arroz, 10, "KG", null));
        registro.agregarBien(bien(arroz, 5, "KG", null));
        registro.agregarBien(bien(fideos, 3, "PAQUETE", null));

        List<Donacion> generadas = registro.segmentar();

        new RepositorioRegistroDonacion(em)
            .agregarRegistroConDonaciones(registro, generadas);

        em.getTransaction().commit();

        idsDonaciones = generadas.stream()
            .map(Donacion::getIdAsString)
            .toList();
      } finally {
        if (em.getTransaction().isActive()) {
          em.getTransaction().rollback();
        }
        em.close();
      }

      EntityManager lectura = factory.createEntityManager();

      try {
        RegistroDonacion recuperado =
            lectura.find(RegistroDonacion.class, idRegistro);

        assertNotNull(recuperado);
        assertEquals(3, recuperado.getListaBienes().size());
        assertEquals(2, idsDonaciones.size());

        Donacion arroz = lectura.find(
            Donacion.class, UUID.fromString(idsDonaciones.get(0))
        );

        Donacion fideos = lectura.find(
            Donacion.class, UUID.fromString(idsDonaciones.get(1))
        );

        assertNotNull(arroz);
        assertNotNull(fideos);
        assertEquals(15, arroz.getCantidad());
        assertEquals(3, fideos.getCantidad());
        assertEquals("ARROZ", arroz.getSubcategoria().getId());
        assertEquals("FIDEOS", fideos.getSubcategoria().getId());
        assertEquals(
            recuperado.getDonante().getId(),
            arroz.getDonante().getId()
        );
      } finally {
        lectura.close();
      }
    } finally {
      factory.close();
    }
  }

  private Bien bien(
      Subcategoria subcategoria,
      int cantidad,
      String unidad,
      LocalDate vencimiento
  ) {
    return new Bien(
        UUID.randomUUID().toString(),
        subcategoria.getNombre(),
        cantidad,
        unidad,
        subcategoria,
        vencimiento,
        null
    );
  }
}