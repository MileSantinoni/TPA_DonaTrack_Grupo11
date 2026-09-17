package org.example.dominio.notificacion;

import java.time.LocalDateTime;
import java.util.UUID;

public class Notificacion {

  private String id;
  private String destinatario;
  private String mensaje;
  private EstadoNotificacion estado;
  private LocalDateTime fechaEnvio;

  public Notificacion(String destinatario, String mensaje) {
    this.id = UUID.randomUUID().toString();
    this.destinatario = destinatario;
    this.mensaje = mensaje;
    this.estado = EstadoNotificacion.PENDIENTE;
  }

  public void marcarComoCompletada() {
    this.estado = EstadoNotificacion.COMPLETADA;
    this.fechaEnvio = LocalDateTime.now();
  }

  public void marcarComoFallida() {
    this.estado = EstadoNotificacion.FALLIDA;
    this.fechaEnvio = LocalDateTime.now();
  }

  public String getId() {
    return id;
  }

  public String getDestinatario() {
    return destinatario;
  }

  public String getMensaje() {
    return mensaje;
  }

  public EstadoNotificacion getEstado() {
    return estado;
  }

  public LocalDateTime getFechaEnvio() {
    return fechaEnvio;
  }
}