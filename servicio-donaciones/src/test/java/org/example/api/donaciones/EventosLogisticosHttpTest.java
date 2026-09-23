package org.example.api.donaciones;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDateTime;
import java.util.*;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.*;
import org.example.dominio.notificacion.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventosLogisticosHttpTest {
  @Test void contratoHttpAplicaEstadoRechazaConflictoYDeduplica() throws Exception {
    var repo = RepositorioAsignacionesDonacion.getInstance(); repo.limpiar();
    var donacion = new Donacion("Arroz", 1, "kg", null, null, null, null);
    var asignacion = donacion.asignarA(new EntidadBeneficiaria("Comedor", "Calle", "123"));
    repo.agregar(asignacion);
    var notificador = new Notificador(new Email(), new SMS(), new WhatsApp()) {
      @Override public void notificarEventoLogistico(AsignacionDonacion a, EventoLogistico e) {}
    };
    var controller = new IntegracionLogisticaController(notificador);
    ObjectMapper json = new ObjectMapper().findAndRegisterModules();
    Javalin app = Javalin.create(c -> c.jsonMapper(new JavalinJackson(json)));
    app.post("/interno/logistica/eventos", controller::registrarEvento); app.start(0);
    try {
      var evento = new EventoLogistico("op1", EventoLogistico.Tipo.INICIO_TRASLADO,
          List.of(new EventoLogistico.Referencia(donacion.getId(), asignacion.getEntidad().getIdAsString())),
          "ABC", "Inicio", LocalDateTime.now());
      var request = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + "/interno/logistica/eventos"))
          .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(evento))).build();
      HttpClient http = HttpClient.newHttpClient();
      assertEquals(409, http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
      donacion.cambiarEstado(EstadoDonacion.LISTA_PARA_ENTREGAR, "Lista");
      assertEquals(204, http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
      assertEquals(204, http.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
      assertEquals(EstadoDonacion.EN_TRASLADO, donacion.getEstadoActual());
      assertEquals(3, donacion.getHistorialEstados().size());
    } finally { app.stop(); repo.limpiar(); }
  }
}
