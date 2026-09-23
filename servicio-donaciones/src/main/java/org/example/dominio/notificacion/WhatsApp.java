package org.example.dominio.notificacion;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class WhatsApp implements TipoNotificacion {

  private final String fromNumber = System.getenv("TWILIO_WHATSAPP_NUMBER");

  @Override
  public Notificacion enviar(String destinatario, String mensaje) {
    Notificacion notificacion = new Notificacion(destinatario, mensaje);
    try {
      // Twilio suele requerir el prefijo "whatsapp:" para este canal
      Message twilioMessage = Message.creator(
          new PhoneNumber("whatsapp:" + destinatario),
          new PhoneNumber("whatsapp:" + fromNumber),
          mensaje
      ).create();

      notificacion.marcarComoCompletada();
      System.out.println("WhatsApp enviado con éxito a: " + destinatario);
    } catch (Exception e) {
      notificacion.marcarComoFallida();
      System.err.println("Error al enviar WhatsApp a: " + destinatario);
      e.printStackTrace();
    }
    return notificacion;
  }
}