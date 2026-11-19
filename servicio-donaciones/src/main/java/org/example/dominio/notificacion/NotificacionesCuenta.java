package org.example.dominio.notificacion;

import javax.persistence.EntityManager;
import org.example.dominio.donante.TipoContactoPredeterminado;
import org.example.seguridad.ContactoCuenta;

public final class NotificacionesCuenta {

  private NotificacionesCuenta() {}

  public static NotificacionPendiente preparar(
      EntityManager em,
      String cuentaId,
      String referencia,
      String operacion,
      String titulo,
      String mensaje
  ) {
    ContactoCuenta contacto = cuentaId == null
        ? null
        : em.find(ContactoCuenta.class, cuentaId);

    TipoContactoPredeterminado canal = null;
    String destino = null;

    if (contacto != null && contacto.canal != null) {
      canal = switch (contacto.canal) {
        case MAIL -> TipoContactoPredeterminado.MAIL;
        case SMS -> TipoContactoPredeterminado.TELEFONO;
        case WHATSAPP -> TipoContactoPredeterminado.WHATSAPP;
      };

      destino = contacto.canal == ContactoCuenta.Canal.MAIL
          ? contacto.email
          : contacto.telefono;
    }

    if (canal == null || destino == null || destino.isBlank()) {
      return NotificacionPendiente.sinContacto(
          operacion,
          cuentaId,
          referencia,
          titulo,
          mensaje
      );
    }

    var pendiente = new NotificacionPendiente(
        operacion,
        canal,
        destino,
        mensaje,
        cuentaId,
        titulo
    );

    pendiente.identificarDestinatario(referencia);
    return pendiente;
  }
}