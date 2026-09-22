package org.example.dominio.notificacion;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import org.example.Repositorios.RepositorioDonantes;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Representante;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.RegistroDonacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.TipoDocumento;
import org.example.scheduler.VerificadorAusenciaDonantes;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UnificacionDonacionesTest {
  private Donante donante() {
    return new Donante("donante@example.org", "123", TipoDocumento.DNI) {};
  }

  @Test
  void asignarDesdeElDominioNotificaUnaVezYSoloSiLaAsignacionEsValida() {
    var donacion = new Donacion("Arroz", 10, "kg", null, null, null, donante());
    var entidad = new EntidadBeneficiaria("Comedor", "Calle 1", "123");
    var beneficiarios = new ArrayList<AsignacionDonacion>();
    var donantes = new ArrayList<AsignacionDonacion>();
    var notificador = new Notificador(new Email(null), new SMS(), new WhatsApp()) {
      @Override public Notificacion notificarDonacionAsignadaBeneficiario(AsignacionDonacion a) {
        beneficiarios.add(a); return null;
      }
      @Override public Notificacion notificarDonacionAsignadaDonante(AsignacionDonacion a) {
        donantes.add(a); return null;
      }
    };
    var asignacion = donacion.asignarA(entidad, notificador);
    assertEquals(EstadoDonacion.ASIGNACION_REALIZADA, donacion.getEstadoActual());
    assertEquals(1, donacion.getHistorialEstados().size());
    assertThrows(IllegalStateException.class, () -> donacion.asignarA(entidad, notificador));
    assertEquals(List.of(asignacion), beneficiarios);
    assertEquals(List.of(asignacion), donantes);
  }

  @Test
  void conservaLaValidacionDeEmailVacioDelMicroservicio() {
    var entidad = new EntidadBeneficiaria("Comedor", "Calle 1", "123");
    entidad.agregarRepresentante(new Representante("A", "A", "   "));
    entidad.agregarRepresentante(new Representante("B", "B", "valido@example.org"));
    var donacion = new Donacion("Arroz", 10, "kg", null, null, null, donante());
    var destinatarios = new ArrayList<String>();
    var email = new Email(null) {
      @Override public Notificacion enviar(String destinatario, String mensaje) {
        destinatarios.add(destinatario); return new Notificacion(destinatario, mensaje);
      }
    };
    var notificador = new Notificador(email, new SMS(), new WhatsApp());
    notificador.notificarDonacionAsignadaBeneficiario(donacion.asignarA(entidad));
    assertEquals(List.of("valido@example.org"), destinatarios);
  }

  @Test
  void elSchedulerDelegaAlDominioSinCapaService() {
    var repo = RepositorioDonantes.getInstance();
    repo.limpiar();
    var notificador = new Notificador(new Email(null), new SMS(), new WhatsApp());
    var llamadas = new ArrayList<LocalDate>();
    var donante = new Donante("donante@example.org", "123", TipoDocumento.DNI) {
      @Override public void notificarAusencia(List<RegistroDonacion> registros,
                                             LocalDate hoy, Notificador recibido) {
        assertSame(notificador, recibido);
        llamadas.add(hoy);
      }
    };
    repo.agregar(donante);
    try {
      new VerificadorAusenciaDonantes(notificador).ejecutar();
      assertEquals(List.of(LocalDate.now()), llamadas);
    } finally {
      repo.limpiar();
    }
  }
}
