package org.example.dominio.notificacion;

import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.RegistroDonacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.Representante;
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
    var notificador = new Notificador(new Email(), new SMS(), new WhatsApp()) {
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
    var email = new Email() {
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
    var notificador = new Notificador(new Email(), new SMS(), new WhatsApp());
    var llamadas = new ArrayList<LocalDate>();
    List<RegistroDonacion> registros = new ArrayList<>();

    var donante = new Donante(
        "donante@example.org", "123", TipoDocumento.DNI
    ) {
      @Override
      public void notificarAusencia(
          List<RegistroDonacion> recibidos,
          LocalDate hoy,
          Notificador recibido
      ) {
        assertSame(registros, recibidos);
        assertSame(notificador, recibido);
        llamadas.add(hoy);
      }
    };

    var verificador = new VerificadorAusenciaDonantes(
        notificador,
        () -> List.of(donante),
        () -> registros
    );

    LocalDate antes = LocalDate.now();
    verificador.ejecutar();
    LocalDate despues = LocalDate.now();

    assertEquals(1, llamadas.size());
    assertTrue(
        llamadas.get(0).equals(antes) || llamadas.get(0).equals(despues)
    );
  }
}
