package org.example.dominio.donacion;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;
import javax.persistence.*;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.beneficiario.*;
import org.example.dominio.catalogo.*;
import org.example.dominio.donante.*;
import org.example.dominio.notificacion.*;
import org.junit.jupiter.api.Test;

class CoberturaNecesidadesTest {

  @Test
  void separaPeriodosSinPerderHistoriaNiDuplicarAportes() {
    var n = new NecesidadRecurrente(
        "Arroz", 20, null,
        LocalDate.of(2026, 1, 31),
        Periodicidad.MENSUAL
    );

    n.actualizarUnidad("kg");

    n.registrarRecepcion(
        "a", 15, LocalDate.of(2026, 2, 27)
    );

    n.registrarRecepcion(
        "a", 15, LocalDate.of(2026, 2, 27)
    );

    n.registrarRecepcion(
        "b", 7, LocalDate.of(2026, 2, 28)
    );

    assertEquals(
        15,
        n.cantidadCubiertaEn(LocalDate.of(2026, 2, 27))
    );

    assertEquals(
        7,
        n.cantidadCubiertaEn(LocalDate.of(2026, 3, 30))
    );

    assertEquals(
        0,
        n.cantidadCubiertaEn(LocalDate.of(2026, 3, 31))
    );

    assertEquals(
        15,
        n.cantidadCubiertaEn(LocalDate.of(2026, 2, 27))
    );

    assertThrows(
        IllegalStateException.class,
        () -> n.registrarRecepcion(
            "a", 99, LocalDate.of(2026, 2, 27)
        )
    );
  }

  @Test
  void asignaNecesidadCompatibleAunqueNoSeaLaPrimera() {
    var sub = new Subcategoria(
        "ARROZ", "Arroz", TipoAtributo.NO_PERECEDERO
    );

    var entidad =
        new EntidadBeneficiaria("Comedor", "Calle", "123");

    var unidades = new NecesidadExtraordinaria(
        "Arroz", 20, sub, "Campaña"
    );
    unidades.actualizarUnidad("unidades");

    var kilos = new NecesidadExtraordinaria(
        "Arroz", 20, sub, "Campaña"
    );
    kilos.actualizarUnidad("kg");

    entidad.registrarNecesidad(unidades);
    entidad.registrarNecesidad(kilos);

    var d = new Donacion(
        "Arroz", 10, "KG", sub,
        null, Estado.NUEVO, donante()
    );

    var asignacion = d.asignarA(entidad);

    assertSame(kilos, asignacion.getNecesidad());
    assertEquals(0, kilos.getCantidadCubierta());
    assertNull(asignacion.getFechaRecepcion());
    assertFalse(unidades.compatibleCon(d));

    kilos.registrarRecepcion(
        "previa", 25, LocalDate.now()
    );

    assertEquals(25, kilos.getCantidadCubierta());
    assertEquals(0, kilos.getCantidadPendiente());
    assertFalse(kilos.puedeRecibir(d, LocalDate.now()));
  }

  @Test
  void recepcionPersisteCoberturaYReintentoNoDuplica() {
    var factory = Persistence.createEntityManagerFactory(
        "donaciones-test",
        Map.of(
            "javax.persistence.jdbc.url",
            "jdbc:hsqldb:mem:cobertura_" + UUID.randomUUID(),
            "hibernate.hbm2ddl.auto",
            "create-drop"
        )
    );

    EntityManager em = factory.createEntityManager();

    try {
      em.getTransaction().begin();

      var sub = new Subcategoria(
          "ARROZ", "Arroz", TipoAtributo.NO_PERECEDERO
      );

      var persona = donante();
      var entidad =
          new EntidadBeneficiaria("Comedor", "Calle", "123");

      var n = new NecesidadExtraordinaria(
          "Arroz", 20, sub, "Campaña"
      );

      n.actualizarUnidad("kg");
      entidad.registrarNecesidad(n);

      em.persist(sub);
      em.persist(persona);
      em.persist(entidad);

      var d = new Donacion(
          "Arroz", 10, "KG", sub,
          null, Estado.NUEVO, persona
      );

      em.persist(d);

      var a = d.asignarA(entidad);
      em.persist(a);

      d.cambiarEstado(
          EstadoDonacion.LISTA_PARA_ENTREGAR, "Lista"
      );
      d.cambiarEstado(
          EstadoDonacion.EN_TRASLADO, "En camino"
      );

      em.getTransaction().commit();

      Long idNecesidad = n.getId();

      var evento = new EventoLogistico(
          "recepcion-1",
          EventoLogistico.Tipo.RECEPCION,
          List.of(
              new EventoLogistico.Referencia(
                  d.getIdAsString(),
                  entidad.getIdAsString()
              )
          ),
          "ABC123",
          "Recibida",
          LocalDateTime.now()
      );

      var notificador = new Notificador(
          new Email(), new SMS(), new WhatsApp()
      );

      new SeguimientoLogistico(
          new RepositorioAsignacionesDonacion(em),
          notificador
      ).registrar(evento);

      em.close();
      em = factory.createEntityManager();

      new SeguimientoLogistico(
          new RepositorioAsignacionesDonacion(em),
          notificador
      ).registrar(evento);

      em.clear();

      assertEquals(
          10,
          em.find(Necesidad.class, idNecesidad)
              .getCantidadCubierta()
      );

      assertEquals(
          EstadoDonacion.ENTREGADA,
          em.find(Donacion.class, d.getId())
              .getEstadoActual()
      );

      assertEquals(
          evento.fecha().toLocalDate(),
          em.find(AsignacionDonacion.class, a.getId())
              .getFechaRecepcion()
      );

    } finally {
      if (em.isOpen()) {
        if (em.getTransaction().isActive()) {
          em.getTransaction().rollback();
        }

        em.close();
      }

      factory.close();
    }
  }

  private PersonaHumana donante() {
    return new PersonaHumana(
        "test@example.org",
        "123",
        TipoDocumento.DNI,
        "Ana",
        "Perez",
        30,
        Genero.OTRO,
        "Calle 1"
    );
  }
}