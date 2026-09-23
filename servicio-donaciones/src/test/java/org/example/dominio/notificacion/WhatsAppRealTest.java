package org.example.dominio.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;



@Disabled("Test manual: envía WhatsApp real con Twilio")
public class WhatsAppRealTest {

  private WhatsApp whatsApp;

  @Test
  void enviaWhatsAppReal() {
    Notificacion notificacion = whatsApp.enviar(
        "+5491161747996",
        "Prueba WhatsApp DonaTrack"
    );

    assertEquals(EstadoNotificacion.COMPLETADA, notificacion.getEstado());
  }
}
