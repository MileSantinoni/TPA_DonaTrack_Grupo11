package org.example.dominio.notificacion;

public interface TipoNotificacion {

  Notificacion enviar(String destinatario, String mensaje);

}