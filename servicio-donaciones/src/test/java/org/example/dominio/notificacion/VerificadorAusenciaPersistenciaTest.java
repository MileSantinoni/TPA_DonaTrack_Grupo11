package org.example.scheduler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.dominio.donacion.RegistroDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificacion;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerificadorAusenciaPersistenciaTest {

  @Test
  void consultaHistorialPersistidoYNotificaSoloAlAusente() {
    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory("donaciones-test");

    try {
      EntityManager em = factory.createEntityManager();

      try {
        em.getTransaction().begin();

        PersonaHumana ausente = crearDonante("ausente@example.org");
        PersonaHumana activo = crearDonante("activo@example.org");
        PersonaHumana sinRegistros = crearDonante("sin-registros@example.org");

        em.persist(ausente);
        em.persist(activo);
        em.persist(sinRegistros);

        LocalDate hoy = LocalDate.now();

        guardarRegistro(
            em, "AUSENTE-21", ausente, hoy.minusDays(21)
        );

        // Tiene una donación antigua y otra reciente:
        // el dominio debe considerar la más reciente.
        guardarRegistro(
            em, "ACTIVO-21", activo, hoy.minusDays(21)
        );
        guardarRegistro(
            em, "ACTIVO-AYER", activo, hoy.minusDays(1)
        );

        em.getTransaction().commit();
      } finally {
        try {
          if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
          }
        } finally {
          em.close();
        }
      }

      // Ya cerramos el contexto que creó los datos.
      List<String> avisados = new ArrayList<>();

      Notificador notificador = new Notificador(
          new Email(), new SMS(), new WhatsApp()
      ) {
        @Override
        public Notificacion notificarDonante(
            Donante donante,
            String mensaje
        ) {
          assertNotNull(donante.getId());
          assertFalse(mensaje.isBlank());
          avisados.add(donante.getMail());

          return new Notificacion(donante.getMail(), mensaje);
        }
      };

      VerificadorAusenciaDonantes verificador =
          new VerificadorAusenciaDonantes(notificador, factory);

      // Ejecución directa: no arranca el temporizador.
      verificador.ejecutar();

      assertEquals(List.of("ausente@example.org"), avisados);
      assertTrue(factory.isOpen());
    } finally {
      factory.close();
    }
  }

  private PersonaHumana crearDonante(String mail) {
    return new PersonaHumana(
        mail,
        mail,
        TipoDocumento.DNI,
        "Persona",
        "Prueba",
        30,
        Genero.OTRO,
        "Calle 1"
    );
  }

  private void guardarRegistro(
      EntityManager em,
      String id,
      Donante donante,
      LocalDate fecha
  ) {
    em.persist(new RegistroDonacion(id, "Registro de prueba", donante));
    em.flush();

    // Preparamos una fecha histórica sin agregar un setter
    // al dominio solamente para el test.
    em.createQuery(
            "UPDATE RegistroDonacion r "
                + "SET r.fechaDeRegistro = :fecha WHERE r.id = :id"
        )
        .setParameter("fecha", fecha)
        .setParameter("id", id)
        .executeUpdate();
  }
}