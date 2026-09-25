package org.example.dominio.notificacion;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donante.*;
import org.example.dominio.donante.Representante;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NotificacionTest {

  private Notificador notificador;
/*
  public Notificador notificadorMock() {
    Email emailFake = new Email(null) {
      @Override
      public Notificacion enviar(String destinatario, String mensaje) {
        Notificacion notificacion = new Notificacion(destinatario, mensaje);
        notificacion.marcarComoCompletada();
        return notificacion;
      }
    };

    return new Notificador(
        emailFake,
        new SMS(),
        new WhatsApp()
    );
  }
 */

  static class DonanteTest extends Donante {
    public DonanteTest(String mail, String nroDoc, TipoDocumento tipo) {
      super(mail, nroDoc, tipo);
    }
  }

  @BeforeEach
  public void setUp() {
    TipoNotificacion fake = (destinatario, mensaje) -> {
      Notificacion notificacion = new Notificacion(destinatario, mensaje);
      notificacion.marcarComoCompletada();
      return notificacion;
    };

    notificador = new Notificador(
        new Email() {
          @Override
          public Notificacion enviar(String destinatario, String mensaje) {
            Notificacion notificacion = new Notificacion(destinatario, mensaje);
            notificacion.marcarComoCompletada();
            return notificacion;
          }
        },
        new SMS() {
          @Override
          public Notificacion enviar(String destinatario, String mensaje) {
            return fake.enviar(destinatario, mensaje);
          }
        },
        new WhatsApp() {
          @Override
          public Notificacion enviar(String destinatario, String mensaje) {
            return fake.enviar(destinatario, mensaje);
          }
        }
    );
  }

  @Test
  public void unaNotificacionRecienCreadaEstaEnEstadoPendiente() {
    Notificacion notificacion = new Notificacion("pepito@gmail.com", "Bienvenido");

    assertEquals(EstadoNotificacion.PENDIENTE, notificacion.getEstado());
  }

  @Test
  public void alMarcarComoCompletadaElEstadoCambia() {
    Notificacion notificacion = new Notificacion("pepito@gmail.com", "Hola");

    notificacion.marcarComoCompletada();

    assertEquals(EstadoNotificacion.COMPLETADA, notificacion.getEstado());
  }

  @Test
  public void alMarcarComoFallidaElEstadoCambia() {
    Notificacion notificacion = new Notificacion("+5491101234567", "Error");

    notificacion.marcarComoFallida();

    assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
  }

  @Test
  public void elEmailEnviaYMarcaLaNotificacionComoCompletada() {

    TipoNotificacion email = new Email() {
      @Override
      public Notificacion enviar(String destinatario, String mensaje) {
        Notificacion notificacion = new Notificacion(destinatario, mensaje);
        notificacion.marcarComoCompletada();
        return notificacion;
      }
    };

    Notificacion resultado = email.enviar(
        "pepito@gmail.com",
        "Tu donación fue recibida"
    );

    assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
    assertEquals("pepito@gmail.com", resultado.getDestinatario());
  }

  @Test
  public void elSMSEnviaYMarcaLaNotificacionComoCompletada() {
    TipoNotificacion sms = (destinatario, mensaje) -> {
      Notificacion notificacion = new Notificacion(destinatario, mensaje);
      notificacion.marcarComoCompletada();
      return notificacion;
    };

    Notificacion resultado = sms.enviar(
        "+5491198765432",
        "Tu donación fue recibida"
    );

    assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
    assertEquals("+5491198765432", resultado.getDestinatario());
  }

  @Test
  public void elWhatsAppEnviaYMarcaLaNotificacionComoCompletada() {
    TipoNotificacion whatsapp = (destinatario, mensaje) -> {
      Notificacion notificacion = new Notificacion(destinatario, mensaje);
      notificacion.marcarComoCompletada();
      return notificacion;
    };

    Notificacion resultado = whatsapp.enviar(
        "+5491198765432",
        "Tu donación fue recibida"
    );

    assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
    assertEquals("+5491198765432", resultado.getDestinatario());
  }

  @Test
  public void siElContactoPredeterminadoEsMailUsaElMailDelDonante() {
    Donante donante = new DonanteTest(
        "pepito@gmail.com",
        "12345678",
        TipoDocumento.DNI
    );

    Notificacion resultado = notificador.notificarDonante(
        donante,
        "Tu donación fue recibida"
    );

    assertEquals("pepito@gmail.com", resultado.getDestinatario());
    assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
  }

  @Test
  public void siElContactoPredeterminadoEsWhatsAppUsaElNumeroDeWhatsApp() {
    Donante donante = new DonanteTest(
        "pepito@gmail.com",
        "12345678",
        TipoDocumento.DNI
    );

    donante.agregarMedioContacto(
        new MedioContacto(TipoMedioContacto.WHATSAPP, "+5491198765432")
    );

    donante.definirContactoPredeterminado(
        TipoContactoPredeterminado.WHATSAPP
    );

    Notificacion resultado = notificador.notificarDonante(
        donante,
        "Tu donación fue recibida"
    );

    assertEquals("+5491198765432", resultado.getDestinatario());
    assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
  }

  @Test
  public void notificarEntidadBeneficiariaUsaElEmailDeSuRepresentante() {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        "Comedor Sol",
        "Av. Siempreviva 742",
        "1140001111"
    );
    entidad.agregarRepresentante(
        new Representante("Ana", "Gomez", "ana@comedorsol.org")
    );

    Notificacion resultado = notificador.notificarEntidadBeneficiaria(
        entidad,
        "La entrega inicio su recorrido"
    );

    assertEquals("ana@comedorsol.org", resultado.getDestinatario());
    assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
  }

  @Test
  public void notificarEntidadBeneficiariaUsaTelefonoSiNoTieneRepresentanteConEmail() {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        "Comedor Sol",
        "Av. Siempreviva 742",
        "1140001111"
    );

    Notificacion resultado = notificador.notificarEntidadBeneficiaria(
        entidad,
        "La entrega inicio su recorrido"
    );

    assertEquals("1140001111", resultado.getDestinatario());
    assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
  }
}
