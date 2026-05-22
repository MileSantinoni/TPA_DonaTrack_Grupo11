package org.example.dominio.notificacion;

import org.example.dominio.donante.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NotificacionTest {

  private Notificador notificador;

  // Subclase concreta de Donante para tests (es abstracta)
  static class DonanteTest extends Donante {
    public DonanteTest(String mail, String nroDoc, TipoDocumento tipo) {
      super(mail, nroDoc, tipo);
    }
  }

  @BeforeEach
  public void setUp() {
    notificador = new Notificador();
  }

  @Test
  public void unaNotificacionRecienCreadaEstaEnEstadoPendiente() {
    Notificacion notificacion = new Notificacion("pepito@gmail.com", "Bienvenido", MedioNotificacion.EMAIL);
    assertEquals(EstadoNotificacion.PENDIENTE, notificacion.getEstado());
  }

  @Test
  public void alMarcarComoCompletadaElEstadoCambia() {
    Notificacion notificacion = new Notificacion("pepito@gmail.com", "Hola", MedioNotificacion.EMAIL);
    notificacion.marcarComoCompletada();
    assertEquals(EstadoNotificacion.COMPLETADA, notificacion.getEstado());
  }

  @Test
  public void alMarcarComoFallidaElEstadoCambia() {
    Notificacion notificacion = new Notificacion("+5491101234567", "Error", MedioNotificacion.SMS);
    notificacion.marcarComoFallida();
    assertEquals(EstadoNotificacion.FALLIDA, notificacion.getEstado());
  }

  @Test
  public void elNotificadorEnviaEmailYLaMarcaComoCompletada() {
    Notificacion resultado = notificador.enviar("pepito@gmail.com", "Tu donación fue recibida", MedioNotificacion.EMAIL);
    assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
  }

  @Test
  public void elNotificadorEnviaSMSYLaMarcaComoCompletada() {
    Notificacion resultado = notificador.enviar("+5491198765432", "Tu donación fue recibida", MedioNotificacion.SMS);
    assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
  }

  @Test
  public void elNotificadorEnviaWhatsAppYLaMarcaComoCompletada() {
    Notificacion resultado = notificador.enviar("+5491198765432", "Tu donación fue recibida", MedioNotificacion.WHATSAPP);
    assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
  }

  @Test
  public void siElContactoPredeterminadoEsMailUsaElMailDelDonante() {
    Donante donante = new DonanteTest("pepito@gmail.com", "12345678", TipoDocumento.DNI);
    // contactoPredeterminado por defecto es MAIL

    Notificacion resultado = notificador.notificarDonante(donante, "Tu donación fue recibida");

    assertEquals("pepito@gmail.com", resultado.getDestinatario());
    assertEquals(MedioNotificacion.EMAIL, resultado.getMedio());
    assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
  }
}
