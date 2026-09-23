package org.example.dominio.donacion;

import java.time.LocalDateTime;
import java.util.*;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donante.*;
import org.example.dominio.notificacion.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class SeguimientoLogisticoTest {
  RepositorioAsignacionesDonacion repo = RepositorioAsignacionesDonacion.getInstance();
  List<String> mensajes = new ArrayList<>();
  Notificador notificador = new Notificador(new Email(), new SMS(), new WhatsApp()) {
    @Override public Notificacion notificarDonante(Donante d, String m) { mensajes.add(m); return null; }
    @Override public Notificacion notificarEntidadBeneficiaria(EntidadBeneficiaria e, String m) { mensajes.add(m); return null; }
  };
  SeguimientoLogistico seguimiento = new SeguimientoLogistico(repo, notificador);
  @BeforeEach void preparar() { repo.limpiar(); }
  @AfterEach void limpiar() { repo.limpiar(); }
  AsignacionDonacion asignar(boolean lista) {
    Donante donante = new Donante("test@example.org", "123", TipoDocumento.DNI) {};
    Donacion donacion = new Donacion("Arroz", 10, "kg", null, null, null, donante);
    var a = donacion.asignarA(new EntidadBeneficiaria("Comedor", "Calle 1", "123"));
    repo.agregar(a);
    if (lista) donacion.cambiarEstado(EstadoDonacion.LISTA_PARA_ENTREGAR, "Lista");
    return a;
  }
  EventoLogistico evento(EventoLogistico.Tipo tipo, AsignacionDonacion... asignaciones) {
    return new EventoLogistico(UUID.randomUUID().toString(), tipo,
        Arrays.stream(asignaciones).map(a -> new EventoLogistico.Referencia(a.getDonacion().getId(), a.getEntidad().getIdAsString())).toList(),
        "ABC", "Motivo", LocalDateTime.now());
  }
  @Test void inicioEnLoteValidaTodosAntesDeCambiarElPrimero() {
    var a = asignar(true); var otra = asignar(false);
    assertThrows(IllegalStateException.class, () -> seguimiento.registrar(evento(EventoLogistico.Tipo.INICIO_TRASLADO, a, otra)));
    assertEquals(EstadoDonacion.LISTA_PARA_ENTREGAR, a.getDonacion().getEstadoActual());
    assertTrue(mensajes.isEmpty());
  }
  @Test void inicioYRecepcionNotificanAmbasPartesYNoDuplicanEnReintentos() {
    var a = asignar(true);
    var inicio = evento(EventoLogistico.Tipo.INICIO_TRASLADO, a);
    seguimiento.registrar(inicio); seguimiento.registrar(inicio);
    assertEquals(EstadoDonacion.EN_TRASLADO, a.getDonacion().getEstadoActual());
    assertEquals(2, mensajes.size());
    var recepcion = evento(EventoLogistico.Tipo.RECEPCION, a);
    seguimiento.registrar(recepcion); seguimiento.registrar(recepcion);
    assertEquals(EstadoDonacion.ENTREGADA, a.getDonacion().getEstadoActual());
    assertEquals(4, mensajes.size());
    assertTrue(mensajes.get(2).contains("ABC"));
    assertTrue(mensajes.get(2).contains(recepcion.fecha().toString()));
    assertEquals(4, a.getDonacion().getHistorialEstados().size());
  }
  @Test void fallidaYRetornoRespetanStateYNotificaciones() {
    var a = asignar(true);
    seguimiento.registrar(evento(EventoLogistico.Tipo.INICIO_TRASLADO, a));
    seguimiento.registrar(evento(EventoLogistico.Tipo.NO_RECIBIDA, a));
    assertEquals(EstadoDonacion.ENTREGA_FALLIDA, a.getDonacion().getEstadoActual());
    seguimiento.registrar(evento(EventoLogistico.Tipo.RETORNO_DEPOSITO, a));
    assertEquals(EstadoDonacion.EN_DEPOSITO, a.getDonacion().getEstadoActual());
    assertEquals(4, mensajes.size());
  }
  @Test void rechazaEntidadIncorrectaYNoReutilizaIdsConOtrosDatos() {
    var a = asignar(true); var inicio = evento(EventoLogistico.Tipo.INICIO_TRASLADO, a);
    var incorrecto = new EventoLogistico("otro", inicio.tipo(), List.of(new EventoLogistico.Referencia(a.getDonacion().getId(), "NO")), "ABC", "Motivo", inicio.fecha());
    assertThrows(IllegalStateException.class, () -> seguimiento.registrar(incorrecto));
    seguimiento.registrar(inicio);
    var repetido = new EventoLogistico(inicio.idOperacion(), inicio.tipo(), inicio.entregas(), "OTRA", inicio.motivo(), inicio.fecha());
    assertThrows(IllegalStateException.class, () -> seguimiento.registrar(repetido));
    assertEquals(2, mensajes.size());
  }
}
