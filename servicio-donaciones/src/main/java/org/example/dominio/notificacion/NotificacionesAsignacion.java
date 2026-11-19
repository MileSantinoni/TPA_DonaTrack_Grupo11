package org.example.dominio.notificacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.EntityManager;

import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donante.TipoContactoPredeterminado;
import org.example.seguridad.ContactoCuenta;
import org.example.seguridad.CuentaWeb;

public final class NotificacionesAsignacion {

  private NotificacionesAsignacion() {
  }

  public static List<NotificacionPendiente> preparar(
      EntityManager em,
      AsignacionDonacion asignacion
  ) {
    var donacion = asignacion.getDonacion();
    var entidad = asignacion.getEntidad();

    String operacion =
        "DONACION_ASIGNADA:" + asignacion.getIdAsString();

    String descripcion = Objects.toString(
        donacion.getDescripcionGeneral(),
        "Donación"
    );

    List<NotificacionPendiente> pendientes = new ArrayList<>();

    CuentaWeb cuentaEntidad = em.createQuery(
            "SELECT c FROM CuentaWeb c "
                + "WHERE c.entidad.id = :id",
            CuentaWeb.class
        )
        .setParameter("id", entidad.getId())
        .getResultStream()
        .findFirst()
        .orElse(null);

    pendientes.add(prepararParaCuenta(
        em,
        cuentaEntidad,
        "ENTIDAD:" + entidad.getId(),
        operacion,
        "Tenés una nueva donación asignada",
        "Se asignó la donación «" + descripcion + "» a "
            + entidad.getRazonSocial()
            + " para cubrir sus necesidades. "
            + "Podés consultar el detalle en Mi entidad."
    ));

    var donante = donacion.getDonante();

    // Los registros históricos pueden no tener un donante asociado.
    if (donante != null) {
      CuentaWeb cuentaDonante = em.createQuery(
              "SELECT c FROM CuentaWeb c "
                  + "WHERE c.donante.id = :id",
              CuentaWeb.class
          )
          .setParameter("id", donante.getId())
          .getResultStream()
          .findFirst()
          .orElse(null);

      pendientes.add(prepararParaCuenta(
          em,
          cuentaDonante,
          "DONANTE:" + donante.getId(),
          operacion,
          "Tu donación ya tiene una entidad asignada",
          "Tu donación «" + descripcion + "» fue asignada a "
              + entidad.getRazonSocial()
              + ". Podés consultar el detalle en Mis donaciones."
      ));
    }

    return pendientes;
  }

  private static NotificacionPendiente prepararParaCuenta(
      EntityManager em,
      CuentaWeb cuenta,
      String referenciaDestinatario,
      String operacion,
      String titulo,
      String mensaje
  ) {
    ContactoCuenta contacto = cuenta == null
        ? null
        : em.find(ContactoCuenta.class, cuenta.id);

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
          cuenta == null ? null : cuenta.id,
          referenciaDestinatario,
          titulo,
          mensaje
      );
    }

    NotificacionPendiente pendiente = new NotificacionPendiente(
        operacion,
        canal,
        destino,
        mensaje,
        cuenta.id,
        titulo
    );

    pendiente.identificarDestinatario(referenciaDestinatario);

    return pendiente;
  }
}