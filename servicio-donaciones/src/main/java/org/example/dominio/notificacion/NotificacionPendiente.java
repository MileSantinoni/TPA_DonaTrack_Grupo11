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
  @Column(nullable = true)
  private TipoContactoPredeterminado canal;

  @Column(nullable = true, length = 500)
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

  @Column(name = "cuenta_id", length = 512)
  private String cuentaId;

  @Column(length = 180)
  private String titulo;

  @Column(name = "fecha_lectura")
  private LocalDateTime fechaLectura;

  @Column(name = "fecha_ocultada")
  private LocalDateTime fechaOcultada;

  @Column(name = "referencia_destinatario", length = 100)
  private String referenciaDestinatario;

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

  public NotificacionPendiente(
      String idOperacion,
      TipoContactoPredeterminado canal,
      String destinatario,
      String mensaje,
      String cuentaId,
      String titulo
  ) {
    this(idOperacion, canal, destinatario, mensaje);

    if (cuentaId == null || cuentaId.isBlank()) {
      throw new IllegalArgumentException(
          "La cuenta destinataria es obligatoria"
      );
    }

    if (titulo == null || titulo.isBlank()) {
      throw new IllegalArgumentException(
          "El título de la notificación es obligatorio"
      );
    }

    this.cuentaId = cuentaId;
    this.titulo = titulo;
  }

  public boolean requiereEnvio() {
    return estado == EstadoNotificacion.PENDIENTE
        || estado == EstadoNotificacion.FALLIDA;
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

  public String getCuentaId() {
    return cuentaId;
  }

  public String getTitulo() {
    return titulo;
  }

  public LocalDateTime getFechaLectura() {
    return fechaLectura;
  }

  public boolean estaLeida() {
    return fechaLectura != null;
  }

  public void marcarLeida() {
    if (fechaLectura == null) {
      fechaLectura = LocalDateTime.now();
    }
  }

  public static NotificacionPendiente sinContacto(
      String idOperacion,
      String cuentaId,
      String referenciaDestinatario,
      String titulo,
      String mensaje
  ) {
    NotificacionPendiente pendiente = new NotificacionPendiente();

    pendiente.id = UUID.randomUUID().toString();
    pendiente.idOperacion = Objects.requireNonNull(idOperacion);
    pendiente.cuentaId = cuentaId;
    pendiente.referenciaDestinatario =
        Objects.requireNonNull(referenciaDestinatario);
    pendiente.titulo = Objects.requireNonNull(titulo);
    pendiente.mensaje = Objects.requireNonNull(mensaje);
    pendiente.estado = EstadoNotificacion.SIN_CONTACTO;
    pendiente.fechaCreacion = LocalDateTime.now();

    return pendiente;
  }

  public void identificarDestinatario(String referencia) {
    this.referenciaDestinatario = Objects.requireNonNull(referencia);
  }

  public String getReferenciaDestinatario() {
    return referenciaDestinatario;
  }
}