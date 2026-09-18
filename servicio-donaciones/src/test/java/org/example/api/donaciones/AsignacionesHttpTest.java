package org.example.api.donaciones;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import io.javalin.Javalin;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.Repositorios.RepositorioResultadosAlgoritmos;
import org.example.dominio.algoritmo.AlgoritmoMadre;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.service.NotificacionesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsignacionesHttpTest {
  private Javalin app;
  private final RepositorioDonaciones donaciones = RepositorioDonaciones.getInstance();
  private final RepositorioEntidadesBeneficiarias entidades = RepositorioEntidadesBeneficiarias.getInstance();
  private final RepositorioAsignacionesDonacion asignaciones = RepositorioAsignacionesDonacion.getInstance();

  @BeforeEach
  void preparar() {
    donaciones.limpiar();
    entidades.limpiar();
    asignaciones.limpiar();
    RepositorioResultadosAlgoritmos.getInstance().limpiar();
    AsignacionController controller = new AsignacionController(donaciones, entidades,
        RepositorioResultadosAlgoritmos.getInstance(), asignaciones,
        new AlgoritmoMadre(), mock(NotificacionesService.class));
    app = Javalin.create();
    app.post("/asignaciones/confirmar", controller::confirmarAsignacion);
    app.post("/asignaciones/rechazar", controller::rechazarAsignacion);
    app.start(0);
  }

  @AfterEach
  void limpiar() {
    app.stop();
    donaciones.limpiar();
    entidades.limpiar();
    asignaciones.limpiar();
    RepositorioResultadosAlgoritmos.getInstance().limpiar();
  }

  @Test
  void confirmarAsignacionRespondeCreado() throws Exception {
    Donacion donacion = new Donacion("Arroz", 50, "kg", null, null, null, null);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Hogar", "Calle 1", "111");
    donaciones.agregar(donacion);
    entidades.agregar(entidad);
    String cuerpo = "{\"idDonacion\":\"" + donacion.getId() + "\",\"idEntidad\":\""
        + entidad.getId() + "\"}";
    assertEquals(201, post("/asignaciones/confirmar", cuerpo));
    assertEquals(EstadoDonacion.ASIGNACION_REALIZADA, donacion.getEstadoActual());
    assertEquals(1, asignaciones.buscarTodas().size());
  }

  @Test
  void rechazarAsignacionNoCambiaElEstado() throws Exception {
    Donacion donacion = new Donacion("Aceite", 20, "litros", null, null, null, null);
    donaciones.agregar(donacion);
    assertEquals(200, post("/asignaciones/rechazar",
        "{\"idDonacion\":\"" + donacion.getId() + "\",\"motivo\":\"Sin capacidad\"}"));
    assertEquals(EstadoDonacion.EN_DEPOSITO, donacion.getEstadoActual());
  }

  private int post(String path, String body) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + app.port() + path))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(body)).build();
    return HttpClient.newHttpClient().send(request,
        HttpResponse.BodyHandlers.discarding()).statusCode();
  }
}
