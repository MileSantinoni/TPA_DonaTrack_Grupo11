package org.example.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.api.logistica.dto.RutaRequest;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.UbicacionCamion;
import org.junit.jupiter.api.Test;

public class ClienteGeneradorRutasTest {

  private final ClienteGeneradorRutas cliente = new ClienteGeneradorRutas();

  @Test
  public void armaRequestParaElMicroservicioConCamionesYAsignacionesRecibidas() {
    Camion camion = new Camion("AB123CD", 20.0, 2.5, 1200.0);
    AsignacionDisponible asignacion = new AsignacionDisponible("DON-1", "ENT-1",
        "Comedor Sol", "Av. Siempre Viva 742", "1140001111", "LISTA_PARA_ENTREGAR");
    UbicacionCamion deposito = new UbicacionCamion(
        -34.60,
        -58.38,
        0.0,
        LocalDateTime.now()
    );

    ClienteGeneradorRutas.GenerarRutasRequest request = cliente.armarRequest(
        List.of(camion),
        List.of(asignacion),
        deposito
    );

    assertEquals(-34.60, request.getUbicacionDeposito().getLatitud());
    assertEquals(-58.38, request.getUbicacionDeposito().getLongitud());
    assertEquals("AB123CD", request.getCamiones().get(0).getPatente());
    assertEquals(asignacion.idDonacion(), request.getAsignaciones().get(0).getIdDonacion());
    assertEquals("Comedor Sol", request.getAsignaciones().get(0).getRazonSocialEntidad());
    assertEquals("Av. Siempre Viva 742", request.getAsignaciones().get(0).getDireccionEntidad());
  }

  @Test
  public void convierteRespuestaDelMicroservicioEnRutaRequestDelTp() {
    ClienteGeneradorRutas.UbicacionRequest ubicacion =
        new ClienteGeneradorRutas.UbicacionRequest();
    ubicacion.setLatitud(-34.60);
    ubicacion.setLongitud(-58.38);

    ClienteGeneradorRutas.EntregaGeneradaResponse entrega =
        new ClienteGeneradorRutas.EntregaGeneradaResponse();
    entrega.setIdDonacion("DON-1");
    entrega.setRazonSocialEntidad("Comedor Sol");
    entrega.setDireccionEntidad("Av. Siempre Viva 742");
    entrega.setTelefonoEntidad("1140001111");
    entrega.setOrden(1);

    ClienteGeneradorRutas.RutaGeneradaResponse rutaGenerada =
        new ClienteGeneradorRutas.RutaGeneradaResponse();
    rutaGenerada.setPatenteCamion("AB123CD");
    rutaGenerada.setUbicacionDeposito(ubicacion);
    rutaGenerada.setEntregas(List.of(entrega));

    List<RutaRequest> rutas = cliente.convertirRespuesta(
        new ClienteGeneradorRutas.RutaGeneradaResponse[] {rutaGenerada}
    );

    assertEquals(1, rutas.size());
    assertEquals("AB123CD", rutas.get(0).getPatente());
    assertEquals(-34.60, rutas.get(0).getLatitudDeposito());
    assertEquals(-58.38, rutas.get(0).getLongitudDeposito());
    assertEquals("DON-1", rutas.get(0).getEntregas().get(0).getIdDonacion());
    assertEquals("Comedor Sol", rutas.get(0).getEntregas().get(0).getRazonSocial());
    assertEquals(1, rutas.get(0).getEntregas().get(0).getOrden());
  }

  @Test
  void solicitaPorHttpManteniendoElContratoDelGenerador() throws Exception {
    io.javalin.Javalin generador = io.javalin.Javalin.create();
    java.util.List<com.fasterxml.jackson.databind.JsonNode> recibidos = new java.util.ArrayList<>();
    generador.post("/rutas/generar", ctx -> {
      recibidos.add(new com.fasterxml.jackson.databind.ObjectMapper().readTree(ctx.body()));
      ctx.json(java.util.List.of(java.util.Map.of(
          "patenteCamion", "ABC", "ubicacionDeposito", java.util.Map.of("latitud", -34.6, "longitud", -58.38),
          "entregas", java.util.List.of(java.util.Map.of("idDonacion", "D1", "razonSocialEntidad", "Comedor",
              "direccionEntidad", "Calle", "telefonoEntidad", "123", "orden", 1)))));
    });
    generador.start(0);
    try {
      var clienteHttp = new ClienteGeneradorRutas(java.net.http.HttpClient.newHttpClient(),
          "http://localhost:" + generador.port() + "/rutas/generar",
          org.example.Repositorios.RepositorioCamiones.getInstance(), new ClienteDonaciones("http://localhost:1"));
      var resultado = clienteHttp.solicitarRutas(
          java.util.List.of(new Camion("ABC", 20, 3, 1000)),
          java.util.List.of(new AsignacionDisponible("D1", "E1", "Comedor", "Calle", "123", "LISTA_PARA_ENTREGAR")),
          new UbicacionCamion(-34.6, -58.38, 0, java.time.LocalDateTime.now()));
      assertEquals("D1", recibidos.get(0).get("asignaciones").get(0).get("idDonacion").asText());
      assertEquals("LISTA_PARA_ENTREGAR", recibidos.get(0).get("asignaciones").get(0).get("estadoDonacion").asText());
      assertEquals("ABC", resultado.get(0).getPatente());
      assertEquals("D1", resultado.get(0).getEntregas().get(0).getIdDonacion());
      assertEquals(-34.6, resultado.get(0).getLatitudDeposito());
    } finally { generador.stop(); }
  }
}
