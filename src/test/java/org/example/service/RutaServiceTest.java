package org.example.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import org.example.Repositorios.RepositorioCamiones;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.api.logistica.dto.RutaRequest;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.MonitorCamiones;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RutaServiceTest {

  private RepositorioCamiones repositorioCamiones;
  private RepositorioDonaciones repositorioDonaciones;

  @BeforeEach
  public void setUp() {
    repositorioCamiones = RepositorioCamiones.getInstance();
    repositorioDonaciones = RepositorioDonaciones.getInstance();
    repositorioCamiones.limpiar();
    repositorioDonaciones.limpiar();
  }

  @AfterEach
  public void tearDown() {
    repositorioCamiones.limpiar();
    repositorioDonaciones.limpiar();
  }

  @Test
  public void alRegistrarRutaElCamionQuedaNoDisponible() {
    Camion camion = new Camion("AB123CD", 20.0, 2.5, 3500.0);
    Donacion donacion = donacionDePrueba();
    repositorioCamiones.agregar(camion);
    repositorioDonaciones.agregar(donacion);
    RutaService rutaService = new RutaService(new MonitorCamiones());

    boolean registrada = rutaService.registrarRuta(requestPara(camion, donacion));

    assertTrue(registrada);
    assertFalse(camion.estaDisponible());
    assertTrue(repositorioCamiones.buscarDisponibles().isEmpty());
  }

  @Test
  public void noRegistraRutaParaCamionNoDisponible() {
    Camion camion = new Camion("AB123CD", 20.0, 2.5, 3500.0);
    camion.marcarNoDisponible();
    Donacion donacion = donacionDePrueba();
    repositorioCamiones.agregar(camion);
    repositorioDonaciones.agregar(donacion);
    RutaService rutaService = new RutaService(new MonitorCamiones());

    boolean registrada = rutaService.registrarRuta(requestPara(camion, donacion));

    assertFalse(registrada);
  }

  private RutaRequest requestPara(Camion camion, Donacion donacion) {
    RutaRequest request = new RutaRequest();
    request.setPatente(camion.getPatente());

    RutaRequest.EntregaRequest entregaRequest = new RutaRequest.EntregaRequest();
    entregaRequest.setIdDonacion(donacion.getId());
    entregaRequest.setRazonSocial("Comedor Sol");
    entregaRequest.setDireccion("Av. Siempreviva 742");
    entregaRequest.setTelefono("1140001111");
    entregaRequest.setOrden(1);

    request.setEntregas(List.of(entregaRequest));
    return request;
  }

  private Donacion donacionDePrueba() {
    Donante donante = new PersonaHumana(
        "donante@test.com",
        "12345678",
        TipoDocumento.DNI,
        "Juan",
        "Perez",
        30,
        Genero.MASCULINO,
        "Calle 123"
    );
    Subcategoria subcategoria = new Subcategoria(
        UUID.randomUUID().toString(),
        "Alimentos",
        TipoAtributo.PERECEDERO
    );

    return new Donacion(
        "Fideos",
        100,
        "Paquetes",
        subcategoria,
        null,
        null,
        donante
    );
  }
}