package org.example;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.catalogo.Estado;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.junit.jupiter.api.Test;

public class ContratoLogisticaTest {
  @Test
  void exponeAsignacionYExistenciaSinCambiarEstado() throws Exception {
    RepositorioDonaciones.getInstance().limpiar();
    RepositorioAsignacionesDonacion.getInstance().limpiar();
    var donante = new PersonaHumana("a@example.com", "12345678", TipoDocumento.DNI,
        "Ana", "Perez", 30, Genero.OTRO, "Calle 1");
    var subcategoria = new Subcategoria("SUB-1", "Alimentos", TipoAtributo.NO_PERECEDERO);
    var donacion = new Donacion("Fideos", 20, "unidades", subcategoria,
        LocalDate.now(), null, Estado.NUEVO, donante);
    var entidad = new EntidadBeneficiaria("Comedor Sol", "Calle 2", "1111");
    RepositorioDonaciones.getInstance().agregar(donacion);
    var asignacion = donacion.asignarA(entidad);
    RepositorioAsignacionesDonacion.getInstance().agregar(asignacion);

    Javalin app = DonacionesApplication.crearApp();
    app.start(0);
    try {
      HttpClient http = HttpClient.newHttpClient();
      String base = "http://localhost:" + app.port();
      HttpResponse<String> consulta = http.send(HttpRequest.newBuilder(
          URI.create(base + "/interno/asignaciones/" + asignacion.getIdAsString())).GET().build(),
          HttpResponse.BodyHandlers.ofString());
      assertEquals(200, consulta.statusCode());
      var contrato = new ObjectMapper().readTree(consulta.body());
      assertEquals(entidad.getId(), contrato.get("idEntidad").asText());
      assertEquals("ASIGNACION_REALIZADA", contrato.get("estadoDonacion").asText());

      HttpResponse<Void> existencia = http.send(HttpRequest.newBuilder(URI.create(
          base + "/interno/donaciones/" + donacion.getIdAsString() + "/existe"))
          .GET().build(), HttpResponse.BodyHandlers.discarding());
      assertEquals(204, existencia.statusCode());
      HttpResponse<Void> ausente = http.send(HttpRequest.newBuilder(URI.create(
          base + "/interno/donaciones/NO-EXISTE/existe"))
          .GET().build(), HttpResponse.BodyHandlers.discarding());
      assertEquals(404, ausente.statusCode());
      assertEquals("ASIGNACION_REALIZADA", donacion.getEstadoActual().name());
    } finally {
      app.stop();
      RepositorioDonaciones.getInstance().limpiar();
      RepositorioAsignacionesDonacion.getInstance().limpiar();
    }
  }
}
