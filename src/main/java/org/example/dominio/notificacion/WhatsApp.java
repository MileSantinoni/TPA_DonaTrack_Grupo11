package org.example.dominio.notificacion;

public class WhatsApp implements TipoNotificacion {

  @Override
  public Notificacion enviar(String destinatario, String mensaje) {
    Notificacion notificacion = new Notificacion(destinatario, mensaje);

    try {
      System.out.println("[WHATSAPP simulado] Para: " + destinatario + " | Mensaje: " + mensaje);
      notificacion.marcarComoCompletada();
    } catch (Exception e) {
      notificacion.marcarComoFallida();
    }

    return notificacion;
  }
}