package org.example.api.logistica;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import org.example.LogisticaApplication;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioCamiones;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.TipoDocumento;
import org.example.dominio.logistica.MonitorCamiones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class LogisticaHttpTest {

  private Javalin app;
  private int port;
  private HttpClient client;
  private ObjectMapper objectMapper;

  static class DonanteStub extends Donante {
    public DonanteStub(String mail, String nroDoc, TipoDocumento tipo) {
      super(mail, nroDoc, tipo);
    }
  }

  @BeforeEach
  public void setUp() {
    MonitorCamiones.getInstance().limpiar();
    RepositorioCamiones.getInstance().limpiar();
    RepositorioDonaciones.getInstance().limpiar();
    RepositorioEntidadesBeneficiarias.getInstance().limpiar();
    RepositorioAsignacionesDonacion.getInstance().limpiar();

    app = LogisticaApplication.crearApp(org.mockito.Mockito.mock(org.example.dominio.notificacion.Notificador.class));
    app.start(0);
    port = app.port();

    client = HttpClient.newHttpClient();
    objectMapper = new ObjectMapper();
  }

  @AfterEach
  public void tearDown() {
    if (app != null) {
      app.stop();
    }
  }

  @Test
  public void testFlujoCompletoHttpCamionesYRutas() throws Exception {
    String baseUrl = "http://localhost:" + port;

    // 1. Alta de camion por POST /camiones
    String camionJson = "{\"patente\":\"AA100BB\",\"capacidadVolumen\":30.0,\"altura\":3.0,\"capacidadCarga\":5000.0}";
    HttpRequest reqCamion = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/camiones"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(camionJson))
        .build();

    HttpResponse<String> resCamion = client.send(reqCamion, HttpResponse.BodyHandlers.ofString());
    assertEquals(201, resCamion.statusCode());
    assertTrue(resCamion.body().contains("AA100BB"));

    // 2. Registrar donacion y entidad en repositorios para la ruta
    Donante donante = new DonanteStub("donante@test.com", "30123456", TipoDocumento.DNI);
    Donacion donacion = new Donacion("Fideos", 100, "paquetes", null, LocalDate.now().plusDays(30), null, donante);
    donacion.cambiarEstado(org.example.dominio.donacion.EstadoDonacion.ASIGNACION_REALIZADA, "Asignacion");
    donacion.cambiarEstado(org.example.dominio.donacion.EstadoDonacion.LISTA_PARA_ENTREGAR, "Planificacion");
    RepositorioDonaciones.getInstance().agregar(donacion);

    // 3. Registrar ruta por POST /rutas
    String rutaJson = String.format(
        "{\"patente\":\"AA100BB\",\"entregas\":[{\"idDonacion\":\"%s\",\"razonSocial\":\"Comedor Esperanza\",\"direccion\":\"Av Rivadavia 1000\",\"telefono\":\"11223344\",\"orden\":1}]}",
        donacion.getId()
    );

    HttpRequest reqRuta = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/rutas"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(rutaJson))
        .build();

    HttpResponse<String> resRuta = client.send(reqRuta, HttpResponse.BodyHandlers.ofString());
    assertEquals(201, resRuta.statusCode());

    // 4. Iniciar ruta por POST /rutas/{patente}/iniciar
    HttpRequest reqIniciar = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/rutas/AA100BB/iniciar"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.noBody())
        .build();

    HttpResponse<String> resIniciar = client.send(reqIniciar, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resIniciar.statusCode());
    assertEquals("Ruta iniciada", resIniciar.body());
    assertEquals(org.example.dominio.donacion.EstadoDonacion.EN_TRASLADO, donacion.getEstadoActual());
    assertEquals(org.example.dominio.logistica.EstadoEntrega.EN_TRASLADO,
        MonitorCamiones.getInstance().rutaDe("AA100BB").getEntregas().get(0).getEstado());

    // 5. Enviar ubicacion por POST /camiones/ubicacion
    String ubicacionJson = "{\"patente\":\"AA100BB\",\"latitud\":-34.6037,\"longitud\":-58.3816,\"velocidad\":55.0}";
    HttpRequest reqUbicacion = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/camiones/ubicacion"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(ubicacionJson))
        .build();

    HttpResponse<String> resUbicacion = client.send(reqUbicacion, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resUbicacion.statusCode());

    // 6. Consultar ultima ubicacion por GET /camiones/{patente}/ubicacion
    HttpRequest reqGetUbi = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/camiones/AA100BB/ubicacion"))
        .GET()
        .build();

    HttpResponse<String> resGetUbi = client.send(reqGetUbi, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resGetUbi.statusCode());
    Map<?, ?> ubiMap = objectMapper.readValue(resGetUbi.body(), Map.class);
    assertEquals(-34.6037, ((Number) ubiMap.get("latitud")).doubleValue(), 0.0001);
    assertEquals(-58.3816, ((Number) ubiMap.get("longitud")).doubleValue(), 0.0001);

    // 7. Consultar avance por GET /camiones/{patente}/avance
    HttpRequest reqGetAvance = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/camiones/AA100BB/avance"))
        .GET()
        .build();

    HttpResponse<String> resGetAvance = client.send(reqGetAvance, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resGetAvance.statusCode());
    Map<?, ?> avanceMap = objectMapper.readValue(resGetAvance.body(), Map.class);
    assertEquals(0.0, ((Number) avanceMap.get("porcentajeAvance")).doubleValue(), 0.0001);
  }

  @Test
  public void testFlujoConfirmarYRechazarAsignacionHttp() throws Exception {
    String baseUrl = "http://localhost:" + port;

    Donante donante = new DonanteStub("donante2@test.com", "30999888", TipoDocumento.DNI);
    Donacion donacionAceptar = new Donacion("Arroz", 50, "kg", null, LocalDate.now().plusDays(20), null, donante);
    Donacion donacionRechazar = new Donacion("Aceite", 20, "litros", null, LocalDate.now().plusDays(20), null, donante);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Hogar Belgrano", "Cabildo 2000", "11556677");

    RepositorioDonaciones.getInstance().agregar(donacionAceptar);
    RepositorioDonaciones.getInstance().agregar(donacionRechazar);
    RepositorioEntidadesBeneficiarias.getInstance().agregar(entidad);

    // Confirmar asignacion por POST /asignaciones/confirmar
    String confirmarJson = String.format(
        "{\"idDonacion\":\"%s\",\"idEntidad\":\"%s\"}",
        donacionAceptar.getId(),
        entidad.getId()
    );

    HttpRequest reqConfirmar = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/asignaciones/confirmar"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(confirmarJson))
        .build();

    HttpResponse<String> resConfirmar = client.send(reqConfirmar, HttpResponse.BodyHandlers.ofString());
    assertEquals(201, resConfirmar.statusCode());
    assertTrue(resConfirmar.body().contains("ASIGNACION_REALIZADA"));

    // Rechazar asignacion por POST /asignaciones/rechazar
    String rechazarJson = String.format(
        "{\"idDonacion\":\"%s\",\"motivo\":\"No coincide con capacidad de almacenamiento\"}",
        donacionRechazar.getId()
    );

    HttpRequest reqRechazar = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/asignaciones/rechazar"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(rechazarJson))
        .build();

    HttpResponse<String> resRechazar = client.send(reqRechazar, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, resRechazar.statusCode());
    assertEquals("Asignacion rechazada", resRechazar.body());
  }
}
