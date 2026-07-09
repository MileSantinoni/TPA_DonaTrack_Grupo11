package org.example.dominio.notificacion;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SMS implements TipoNotificacion {

  @Value("${twilio.smsFrom}")
  private String remitente;

  @Override
  public Notificacion enviar(String destinatario, String mensaje) {
    Notificacion notificacion = new Notificacion(destinatario, mensaje);

    try {
      Message.creator(
          new PhoneNumber(destinatario),
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