package org.example.generadorrutas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.example.generadorrutas.dto.AsignacionRutaRequest;
import org.example.generadorrutas.dto.CamionRutaRequest;
import org.example.generadorrutas.dto.EstadoDonacionDto;
import org.example.generadorrutas.dto.GenerarRutasRequest;
import org.example.generadorrutas.dto.RutaGeneradaResponse;
import org.example.generadorrutas.dto.UbicacionRequest;
import org.junit.jupiter.api.Test;

public class GeneradorDeRutasServiceTest {

  private final GeneradorDeRutasService service = new GeneradorDeRutasService();

  @Test
  public void agrupaDonacionesDeLaMismaEntidadYLasAsignaACamionesDisponibles() {
    GenerarRutasRequest request = new GenerarRutasRequest();
    request.setUbicacionDeposito(ubicacionDeposito());
    request.setCamiones(List.of(
        camion("AB123CD", true),
        camion("CD456EF", true)
    ));
    request.setAsignaciones(List.of(
        asignacion("DON-1", "Comedor Sol", EstadoDonacionDto.ASIGNACION_REALIZADA),
        asignacion("DON-2", "Hogar Luz", EstadoDonacionDto.ASIGNACION_REALIZADA),
        asignacion("DON-3", "Comedor Sol", EstadoDonacionDto.ASIGNACION_REALIZADA)
    ));

    List<RutaGeneradaResponse> rutas = service.generar(request);

    assertEquals(2, rutas.size());
    assertEquals("AB123CD", rutas.get(0).getPatenteCamion());
    assertEquals("CD456EF", rutas.get(1).getPatenteCamion());
    assertEquals(2, rutas.get(0).getEntregas().size());
    assertEquals(1, rutas.get(1).getEntregas().size());
    assertEquals("DON-1", rutas.get(0).getEntregas().get(0).getIdDonacion());
    assertEquals("DON-3", rutas.get(0).getEntregas().get(1).getIdDonacion());
    assertEquals(1, rutas.get(0).getEntregas().get(0).getOrden());
    assertEquals(2, rutas.get(0).getEntregas().get(1).getOrden());
  }

  @Test
  public void usaLasAsignacionesYCamionesQueRecibeSinVolverAFiltrar() {
    GenerarRutasRequest request = new GenerarRutasRequest();
    request.setCamiones(List.of(
        camion("AB123CD", true),
        camion("CD456EF", false)
    ));
    request.setAsignaciones(List.of(
        asignacion("DON-1", "Comedor Sol", EstadoDonacionDto.ASIGNACION_REALIZADA),
        asignacion("DON-2", "Hogar Luz", EstadoDonacionDto.EN_DEPOSITO)
    ));

    List<RutaGeneradaResponse> rutas = service.generar(request);

    assertEquals(2, rutas.size());
    assertEquals("AB123CD", rutas.get(0).getPatenteCamion());
    assertEquals("CD456EF", rutas.get(1).getPatenteCamion());
    assertEquals(1, rutas.get(0).getEntregas().size());
    assertEquals(1, rutas.get(1).getEntregas().size());
    assertEquals("DON-1", rutas.get(0).getEntregas().get(0).getIdDonacion());
    assertEquals("DON-2", rutas.get(1).getEntregas().get(0).getIdDonacion());
  }

  @Test
  public void devuelveListaVaciaSiNoRecibeCamiones() {
    GenerarRutasRequest request = new GenerarRutasRequest();
    request.setCamiones(List.of());
    request.setAsignaciones(List.of(
        asignacion("DON-1", "Comedor Sol", EstadoDonacionDto.ASIGNACION_REALIZADA)
    ));

    List<RutaGeneradaResponse> rutas = service.generar(request);

    assertTrue(rutas.isEmpty());
  }

  private CamionRutaRequest camion(String patente, boolean disponible) {
    CamionRutaRequest camion = new CamionRutaRequest();
    camion.setPatente(patente);
    camion.setDisponible(disponible);
    return camion;
  }

  private AsignacionRutaRequest asignacion(
      String idDonacion,
      String razonSocial,
      EstadoDonacionDto estado
  ) {
    AsignacionRutaRequest asignacion = new AsignacionRutaRequest();
    asignacion.setIdDonacion(idDonacion);
    asignacion.setEstadoDonacion(estado);
    asignacion.setRazonSocialEntidad(razonSocial);
    asignacion.setDireccionEntidad("Direccion " + razonSocial);
    asignacion.setTelefonoEntidad("1140001111");
    return asignacion;
  }

  private UbicacionRequest ubicacionDeposito() {
    UbicacionRequest ubicacion = new UbicacionRequest();
    ubicacion.setLatitud(-34.60);
    ubicacion.setLongitud(-58.38);
    return ubicacion;
  }
}
