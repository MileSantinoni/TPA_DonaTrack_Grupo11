package org.example.dominio.notificacion;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.persistence.*;
import org.example.dominio.donante.TipoContactoPredeterminado;

@Entity
@Table(name = "notificaciones_pendientes")
public class NotificacionPendiente {

  @Id
  private String id;

  @Column(name = "id_operacion", nullable = false)
  private String idOperacion;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TipoContactoPredeterminado canal;

  @Column(nullable = false, length = 500)
  private String destinatario;

  @Column(nullable = false, length = 10000)
  private String mensaje;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoNotificacion estado;

  @Column(nullable = false)
  private int intentos;

  @Column(name = "fecha_creacion", nullable = false)
  private LocalDateTime fechaCreacion;

  @Column(name = "ultimo_intento")
  private LocalDateTime ultimoIntento;

  @Column(name = "fecha_completada")
  private LocalDateTime fechaCompletada;

  @Version
  private long version;

  protected NotificacionPendiente() {
  }

  public NotificacionPendiente(
      String idOperacion,
      TipoContactoPredeterminado canal,
      String destinatario,
      String mensaje
  ) {
    if (idOperacion == null || idOperacion.isBlank()) {
      throw new IllegalArgumentException(
          "El ID de operacion es obligatorio"
      );
    }

    if (destinatario == null || destinatario.isBlank()) {
      throw new IllegalArgumentException(
          "El destinatario es obligatorio"
      );
    }

    if (mensaje == null || mensaje.isBlank()) {
      throw new IllegalArgumentException(
          "El mensaje es obligatorio"
      );
    }

    this.id = UUID.randomUUID().toString();
    this.idOperacion = idOperacion;
    this.canal = Objects.requireNonNull(canal);
    this.destinatario = destinatario;
    this.mensaje = mensaje;
    this.estado = EstadoNotificacion.PENDIENTE;
    this.fechaCreacion = LocalDateTime.now();
  }

  public boolean requiereEnvio() {
    return estado != EstadoNotificacion.COMPLETADA;
  }

  public void registrarResultado(Notificacion resultado) {
    exigirPendiente();

    intentos++;
    ultimoIntento = LocalDateTime.now();

    if (resultado != null
        && resultado.getEstado() == EstadoNotificacion.COMPLETADA) {
      estado = EstadoNotificacion.COMPLETADA;
      fechaCompletada = ultimoIntento;
    } else {
      estado = EstadoNotificacion.FALLIDA;
    }
  }

  public void registrarFallo() {
    registrarResultado(null);
  }

  private void exigirPendiente() {
    if (!requiereEnvio()) {
      throw new IllegalStateException(
          "La notificacion ya fue completada"
      );
    }
  }

  public String getId() {
    return id;
  }

  public String getIdOperacion() {
    return idOperacion;
  }

  public TipoContactoPredeterminado getCanal() {
    return canal;
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

  public int getIntentos() {
    return intentos;
  }

  public LocalDateTime getFechaCreacion() {
    return fechaCreacion;
  }

  public LocalDateTime getUltimoIntento() {
    return ultimoIntento;
  }

  public LocalDateTime getFechaCompletada() {
    return fechaCompletada;
  }
}