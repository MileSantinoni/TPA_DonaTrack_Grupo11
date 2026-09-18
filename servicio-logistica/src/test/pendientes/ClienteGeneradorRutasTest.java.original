package org.example.integracion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.example.api.logistica.dto.RutaRequest;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.catalogo.Estado;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.UbicacionCamion;
import org.junit.jupiter.api.Test;

public class ClienteGeneradorRutasTest {

  private final ClienteGeneradorRutas cliente = new ClienteGeneradorRutas();

  @Test
  public void armaRequestParaElMicroservicioConCamionesYAsignacionesRecibidas() {
    Camion camion = new Camion("AB123CD", 20.0, 2.5, 1200.0);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        "Comedor Sol",
        "Av. Siempre Viva 742",
        "1140001111"
    );
    Donacion donacion = new Donacion(
        "Cajas de alimentos",
        10,
        "cajas",
        null,
        LocalDate.now().plusDays(10),
        Estado.NUEVO,
        null
    );
    AsignacionDonacion asignacion = new AsignacionDonacion(
        donacion,
        entidad,
        null,
        LocalDate.now()
    );
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
    assertEquals(donacion.getId(), request.getAsignaciones().get(0).getIdDonacion());
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
}
