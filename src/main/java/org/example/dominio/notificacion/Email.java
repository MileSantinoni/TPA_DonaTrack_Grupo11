package org.example.dominio.notificacion;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class Email implements TipoNotificacion {

  private final JavaMailSender mailSender;

  public Email(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Override
  public Notificacion enviar(String destinatario, String mensaje) {
    Notificacion notificacion = new Notificacion(destinatario, mensaje);

    try {
      SimpleMailMessage mail = new SimpleMailMessage();
      mail.setTo(destinatario);
      mail.setSubject("DonaTrack");
      mail.setText(mensaje);

      mailSender.send(mail);

      notificacion.marcarComoCompletada();
    } catch (Exception e) {
      e.printStackTrace();
      notificacion.marcarComoFallida();
    }

    return notificacion;
  }
}