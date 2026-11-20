package org.example.dominio.notificacion;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.example.Twilio.TwilioConfig;

public class SMS implements TipoNotificacion {

  private final String fromNumber = System.getenv("TWILIO_PHONE_NUMBER");

  @Override
  public Notificacion enviar(String destinatario, String mensaje) {
    Notificacion notificacion = new Notificacion(destinatario, mensaje);
    try {
      Message twilioMessage = Message.creator(
          new PhoneNumber(destinatario),
          new PhoneNumber(fromNumber),
          mensaje
      ).create(TwilioConfig.cliente());

      notificacion.marcarComoCompletada();
      System.out.println("SMS aceptado por Twilio. SID: " + twilioMessage.getSid());
    } catch (Exception e) {
      notificacion.marcarComoFallida();
      System.err.println("Error al enviar SMS a: " + destinatario);
      e.printStackTrace();
    }
    return notificacion;
  }
}