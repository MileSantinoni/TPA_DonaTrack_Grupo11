package org.example.dominio.notificacion;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WhatsApp implements TipoNotificacion {

  @Value("${twilio.whatsappFrom}")
  private String remitente;

  @Override
  public Notificacion enviar(String destinatario, String mensaje) {
    Notificacion notificacion = new Notificacion(destinatario, mensaje);

    try {
      String destinoWhatsApp = destinatario.startsWith("whatsapp:")
          ? destinatario
          : "whatsapp:" + destinatario;

      Message.creator(
          new PhoneNumber(destinoWhatsApp),
          new PhoneNumber(remitente),
          mensaje
      ).create();

      notificacion.marcarComoCompletada();
    } catch (Exception e) {
      e.printStackTrace();
      notificacion.marcarComoFallida();
    }

    return notificacion;
  }
}