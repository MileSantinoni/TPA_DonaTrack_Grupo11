package org.example.api.logistica;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioCamiones;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.api.donaciones.AsignacionController;
import org.example.api.donaciones.dto.ConfirmarAsignacionRequest;
import org.example.api.donaciones.dto.RechazarAsignacionRequest;
import org.example.api.logistica.dto.AvanceResponse;
import org.example.api.logistica.dto.CamionRequest;
import org.example.api.logistica.dto.ReporteUbicacionRequest;
import org.example.api.logistica.dto.UbicacionResponse;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.TipoDocumento;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.Entrega;
import org.example.dominio.logistica.MonitorCamiones;
import org.example.dominio.logistica.Ruta;
import org.example.service.RutaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class LogisticaApiJavalinTest {
  private final org.example.dominio.notificacion.Notificador notificador = mock(org.example.dominio.notificacion.Notificador.class);

  private MonitorCamiones monitor;
  private RepositorioCamiones repoCamiones;
  private CamionController camionController;
  private RutaController rutaController;
  private AsignacionController asignacionController;

  static class DonanteStub extends Donante {
    public DonanteStub(String mail, String nroDoc, TipoDocumento tipo) {
      super(mail, nroDoc, tipo);
    }
  }

  @BeforeEach
  public void setUp() {
    monitor = MonitorCamiones.getInstance();
    monitor.limpiar();
    repoCamiones = RepositorioCamiones.getInstance();
    repoCamiones.limpiar();
    RepositorioDonaciones.getInstance().limpiar();
    RepositorioEntidadesBeneficiarias.getInstance().limpiar();
    RepositorioAsignacionesDonacion.getInstance().limpiar();

    camionController = new CamionController(monitor, repoCamiones);
    rutaController = new RutaController(new RutaService(monitor), monitor, notificador);
    asignacionController = new AsignacionController();
  }

  @Test
  public void inicioDeRutaInvalidaRespondeConflictoSinActivarla() {
    Camion camion = new Camion("AB123CD", 25, 3, 4000);
    Ruta ruta = new Ruta(camion);
    Donacion donacion = new Donacion("Arroz", 10, "kg", null, null, null, null);
    ruta.agregarEntrega(new Entrega(donacion, new EntidadBeneficiaria("Comedor", "Dir", "123"), 1));
    monitor.registrarRuta(ruta);
    Context ctx = mock(Context.class);
    when(ctx.pathParam("patente")).thenReturn("AB123CD");
    when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);
    rutaController.iniciarRuta(ctx);
    verify(ctx).status(HttpStatus.CONFLICT);
    assertFalse(ruta.estaActiva());
    assertEquals(EstadoDonacion.EN_DEPOSITO, donacion.getEstadoActual());
    verifyNoInteractions(notificador);
  }

  @Test
  public void testRegistrarCamionEndpoint() {
    Context ctx = mock(Context.class);
    CamionRequest req = new CamionRequest();
    req.setPatente("AB123CD");
    req.setCapacidadVolumen(25.0);
    req.setAltura(2.8);
    req.setCapacidadCarga(4000.0);

    when(ctx.bodyAsClass(CamionRequest.class)).thenReturn(req);
    when(ctx.status(any(HttpStatus.class))).thenReturn(ctx);

    camionController.registrarCamion(ctx);

    verify(ctx).status(HttpStatus.CREATED);
    assertNotNull(repoCamiones.buscarPorPatente("AB123CD"));
  }

  @Test
  public void testRecibirUbicacionValidaEInvalidaEndpoint() {
    Camion camion = new Camion("AB123CD", 25.0, 2.8, 4000.0);
    repoCamiones.agregar(camion);
    Ruta ruta = new Ruta(camion);
    ruta.iniciar(notificador);
    monitor.registrarRuta(ruta);

    Context ctxValido = mock(Context.class);
    ReporteUbicacionRequest reqValido = new ReporteUbicacionRequest();
    reqValido.setPatente("AB123CD");
    reqValido.setLatitud(-34.60);
    reqValido.setLongitud(-58.38);
    reqValido.setVelocidad(50.0);
    reqValido.setFechaYHora(LocalDateTime.now());

    when(ctxValido.bodyAsClass(ReporteUbicacionRequest.class)).thenReturn(reqValido);
    when(ctxValido.status(any(HttpStatus.class))).thenReturn(ctxValido);

    camionController.recibirUbicacion(ctxValido);
    verify(ctxValido).status(HttpStatus.OK);
    assertEquals(-34.60, monitor.ubicacionActual("AB123CD").getLatitud());

    Context ctxInvalido = mock(Context.class);
    ReporteUbicacionRequest reqInvalido = new ReporteUbicacionRequest();
    reqInvalido.setPatente("AB123CD");
    reqInvalido.setLatitud(200.0);
    reqInvalido.setLongitud(-58.38);
    reqInvalido.setVelocidad(50.0);
    reqInvalido.setFechaYHora(LocalDateTime.now());

    when(ctxInvalido.bodyAsClass(ReporteUbicacionRequest.class)).thenReturn(reqInvalido);
    when(ctxInvalido.status(any(HttpStatus.class))).thenReturn(ctxInvalido);

    camionController.recibirUbicacion(ctxInvalido);
    verify(ctxInvalido).status(HttpStatus.BAD_REQUEST);
  }

  @Test
  public void testUbicacionActualYAvanceEndpoint() {
    Camion camion = new Camion("AB123CD", 25.0, 2.8, 4000.0);
    repoCamiones.agregar(camion);
    Ruta ruta = new Ruta(camion);

    Donante donante = new DonanteStub("santi@mail.com", "40111222", TipoDocumento.DNI);
    Donacion d1 = new Donacion("Leche", 50, "litros", null, LocalDate.now().plusDays(20), null, donante);
    Donacion d2 = new Donacion("Arroz", 30, "kg", null, LocalDate.now().plusDays(20), null, donante);

    EntidadBeneficiaria ent1 = new EntidadBeneficiaria("Comedor 1", "Dir 1", "123");
    EntidadBeneficiaria ent2 = new EntidadBeneficiaria("Comedor 2", "Dir 2", "456");
    for (Donacion donacion : java.util.List.of(d1, d2)) {
      donacion.cambiarEstado(EstadoDonacion.ASIGNACION_REALIZADA, "Asignacion");
      donacion.cambiarEstado(EstadoDonacion.LISTA_PARA_ENTREGAR, "Planificacion");
    }
    Entrega e1 = new Entrega(d1, ent1, 1);
    Entrega e2 = new Entrega(d2, ent2, 2);
    ruta.agregarEntrega(e1);
    ruta.agregarEntrega(e2);
    ruta.iniciar(notificador);
    monitor.registrarRuta(ruta);

    ReporteUbicacionRequest req = new ReporteUbicacionRequest();
    req.setPatente("AB123CD");
    req.setLatitud(-34.60);
    req.setLongitud(-58.38);
    req.setVelocidad(50.0);
    req.setFechaYHora(LocalDateTime.now());

    Context ctxReporte = mock(Context.class);
    when(ctxReporte.bodyAsClass(ReporteUbicacionRequest.class)).thenReturn(req);
    when(ctxReporte.status(any(HttpStatus.class))).thenReturn(ctxReporte);
    camionController.recibirUbicacion(ctxReporte);

    Context ctxUbicacion = mock(Context.class);
    when(ctxUbicacion.pathParam("patente")).thenReturn("AB123CD");
    when(ctxUbicacion.status(any(HttpStatus.class))).thenReturn(ctxUbicacion);

    camionController.ubicacionActual(ctxUbicacion);
    verify(ctxUbicacion).status(HttpStatus.OK);
    verify(ctxUbicacion).json(any(UbicacionResponse.class));

    e1.confirmarRecepcion(camion, notificador);
    Context ctxAvance = mock(Context.class);
    when(ctxAvance.pathParam("patente")).thenReturn("AB123CD");
    when(ctxAvance.status(any(HttpStatus.class))).thenReturn(ctxAvance);

    camionController.avanceDeRuta(ctxAvance);
    verify(ctxAvance).status(HttpStatus.OK);
    verify(ctxAvance).json(any(AvanceResponse.class));
    assertEquals(50.0, monitor.avanceDeRuta("AB123CD"));
  }

  @Test
  public void testAsignacionAceptarYRechazarEndpoints() {
    Donante donante = new DonanteStub("santi@mail.com", "40111222", TipoDocumento.DNI);
    Donacion donacion = new Donacion("Leche", 50, "litros", null, LocalDate.now().plusDays(20), null, donante);
    EntidadBeneficiaria entidad = new EntidadBeneficiaria("Hogar Niño", "Calle 123", "1122334455");

    RepositorioDonaciones.getInstance().agregar(donacion);
    RepositorioEntidadesBeneficiarias.getInstance().agregar(entidad);

    // Test Aceptar Asignacion
    Context ctxConfirmar = mock(Context.class);
    ConfirmarAsignacionRequest reqConfirmar = new ConfirmarAsignacionRequest();
    reqConfirmar.setIdDonacion(donacion.getId());
    reqConfirmar.setIdEntidad(entidad.getId());

    when(ctxConfirmar.bodyAsClass(ConfirmarAsignacionRequest.class)).thenReturn(reqConfirmar);
    when(ctxConfirmar.status(any(HttpStatus.class))).thenReturn(ctxConfirmar);

    asignacionController.confirmarAsignacion(ctxConfirmar);
    verify(ctxConfirmar).status(HttpStatus.CREATED);
    assertEquals(EstadoDonacion.ASIGNACION_REALIZADA, donacion.getEstadoActual());
    assertEquals(1, RepositorioAsignacionesDonacion.getInstance().buscarTodas().size());

    // Test Rechazar Asignacion para una nueva donacion
    Donacion donacion2 = new Donacion("Fideos", 30, "kg", null, LocalDate.now().plusDays(20), null, donante);
    RepositorioDonaciones.getInstance().agregar(donacion2);

    Context ctxRechazar = mock(Context.class);
    RechazarAsignacionRequest reqRechazar = new RechazarAsignacionRequest();
    reqRechazar.setIdDonacion(donacion2.getId());
    reqRechazar.setMotivo("Capacidad superada");

    when(ctxRechazar.bodyAsClass(RechazarAsignacionRequest.class)).thenReturn(reqRechazar);
    when(ctxRechazar.status(any(HttpStatus.class))).thenReturn(ctxRechazar);

    asignacionController.rechazarAsignacion(ctxRechazar);
    verify(ctxRechazar).status(HttpStatus.OK);
    assertEquals(EstadoDonacion.EN_DEPOSITO, donacion2.getEstadoActual());
  }
}