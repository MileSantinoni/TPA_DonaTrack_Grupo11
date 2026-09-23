package org.example.dominio.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;



@Disabled("Test manual: envía SMS real con Twilio")
public class SmsRealTest {


  private SMS sms;

  @Test
  void enviaSmsReal() {
    Notificacion notificacion = sms.enviar(
        "+5491161747996",
        "Prueba SMS DonaTrack"
    );

    assertEquals(EstadoNotificacion.COMPLETADA, notificacion.getEstado());
  }
}
