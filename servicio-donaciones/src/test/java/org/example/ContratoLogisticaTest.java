package org.example;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import javax.persistence.*;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.catalogo.Estado;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.dominio.donante.*;
import org.example.server.Server;
import org.junit.jupiter.api.Test;

public class ContratoLogisticaTest {

  @Test
  void exponeAsignacionYExistenciaSinCambiarEstado() throws Exception {
    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory(
            "simple-persistence-unit",
            Map.of(
                "javax.persistence.jdbc.driver",
                "org.hsqldb.jdbc.JDBCDriver",
                "javax.persistence.jdbc.url",
                "jdbc:hsqldb:mem:contrato_" + UUID.randomUUID(),
                "javax.persistence.jdbc.user", "sa",
                "javax.persistence.jdbc.password", "",
                "hibernate.dialect",
                "org.hibernate.dialect.HSQLDialect",
                "hibernate.hbm2ddl.auto", "create-drop"
            )
        );

    Javalin app = null;

    try {
      String idDonacion;
      String idEntidad;
      String idAsignacion;
      int historialInicial;

      EntityManager em = factory.createEntityManager();

      try {
        em.getTransaction().begin();

        var donante = new PersonaHumana(
            "a@example.com", "12345678", TipoDocumento.DNI,
            "Ana", "Perez", 30, Genero.OTRO, "Calle 1"
        );

        var subcategoria = new Subcategoria(
            "SUB-1", "Alimentos", TipoAtributo.NO_PERECEDERO
        );

        var entidad =
            new EntidadBeneficiaria("Comedor Sol", "Calle 2", "1111");

        entidad.confirmarUbicacion(-34.596118, -58.394256);
        em.persist(donante);
        em.persist(subcategoria);
        em.persist(entidad);

        var donacion = new Donacion(
            "Fideos", 20, "unidades", subcategoria,
            LocalDate.now(), null, Estado.NUEVO, donante
        );

        em.persist(donacion);

        var asignacion = donacion.asignarA(entidad);

        new RepositorioAsignacionesDonacion(em).agregar(asignacion);

        em.getTransaction().commit();

        idDonacion = donacion.getIdAsString();
        idEntidad = entidad.getIdAsString();
        idAsignacion = asignacion.getIdAsString();
        historialInicial = donacion.getHistorialEstados().size();
      } finally {
        try {
          if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
          }
        } finally {
          em.close();
        }
      }

      // La aplicación consulta la misma base donde preparamos los datos.
      app = Server.crearApp(factory);
      app.start(0);

      HttpClient http = HttpClient.newHttpClient();
      String base = "http://localhost:" + app.port();

      HttpResponse<String> consulta = http.send(
          HttpRequest.newBuilder(
              URI.create(base + "/interno/asignaciones/" + idAsignacion)
          ).GET().build(),
          HttpResponse.BodyHandlers.ofString()
      );

      assertEquals(200, consulta.statusCode(), consulta.body());

      var contrato = new ObjectMapper().readTree(consulta.body());

      assertEquals(-34.596118, contrato.get("latitud").doubleValue(), 0.000001);
      assertEquals(-58.394256, contrato.get("longitud").doubleValue(), 0.000001);
      assertEquals(idEntidad, contrato.get("idEntidad").asText());
      assertEquals(idDonacion, contrato.get("idDonacion").asText());
      assertEquals(
          "ASIGNACION_REALIZADA",
          contrato.get("estadoDonacion").asText()
      );

      HttpResponse<String> existencia = http.send(
          HttpRequest.newBuilder(
              URI.create(
                  base + "/interno/donaciones/" + idDonacion + "/existe"
              )
          ).GET().build(),
          HttpResponse.BodyHandlers.ofString()
      );

      assertEquals(204, existencia.statusCode(), existencia.body());

      HttpResponse<String> ausente = http.send(
          HttpRequest.newBuilder(
              URI.create(base + "/interno/donaciones/NO-EXISTE/existe")
          ).GET().build(),
          HttpResponse.BodyHandlers.ofString()
      );

      assertEquals(404, ausente.statusCode());

      EntityManager verificacion = factory.createEntityManager();

      try {
        Donacion recuperada =
            new RepositorioDonaciones(verificacion)
                .buscarPorId(idDonacion)
                .orElseThrow();

        assertEquals(
            EstadoDonacion.ASIGNACION_REALIZADA,
            recuperada.getEstadoActual()
        );

        assertEquals(
            historialInicial,
            recuperada.getHistorialEstados().size()
        );
      } finally {
        verificacion.close();
      }
    } finally {
      try {
        if (app != null) {
          app.stop();
        }
      } finally {
        if (factory.isOpen()) {
          factory.close();
        }
      }
    }
  }
}