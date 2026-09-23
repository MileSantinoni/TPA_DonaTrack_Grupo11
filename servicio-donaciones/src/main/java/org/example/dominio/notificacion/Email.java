package org.example.dominio.notificacion;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class Email implements TipoNotificacion {

  private final String username = System.getenv("MAIL_USERNAME");
  private final String password = System.getenv("MAIL_PASSWORD");

  @Override
  public Notificacion enviar(String destinatario, String mensaje) {
    // 1. Creamos la notificación
    Notificacion notificacion = new Notificacion(destinatario, mensaje);

    Properties prop = new Properties();
    prop.put("mail.smtp.host", "smtp.gmail.com");
    prop.put("mail.smtp.port", "587");
    prop.put("mail.smtp.auth", "true");
    prop.put("mail.smtp.starttls.enable", "true");

    Session session = Session.getInstance(prop, new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
      }
    });

    try {
      Message mimeMessage = new MimeMessage(session);
      if (username != null) {
        mimeMessage.setFrom(new InternetAddress(username));
      }

      mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
      mimeMessage.setSubject("Aviso de DonaTrack");
      mimeMessage.setText(mensaje);

      Transport.send(mimeMessage);

      // 2. Marcamos como completada
      notificacion.marcarComoCompletada();
      System.out.println("Email enviado con éxito a: " + destinatario);

    } catch (MessagingException e) {
      // 3. Marcamos como fallida si ocurre un error
      notificacion.marcarComoFallida();
      System.err.println("Error al enviar el email a: " + destinatario);
      e.printStackTrace();
    }

    // 4. Retornamos el objeto para que el Notificador no falle
    return notificacion;
  }
}