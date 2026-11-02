package org.example.dominio.notificacion;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.MedioContacto;
import org.example.dominio.donante.TipoContactoPredeterminado;
import org.example.dominio.donante.TipoMedioContacto;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donante.Representante;

import java.util.ArrayList;
import org.example.dominio.donacion.EventoLogistico;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Notificador {

  private final Map<TipoContactoPredeterminado, TipoNotificacion> estrategias;

//  public Notificador() {
//    this(new Email(null), new SMS(), new WhatsApp());
//  }

  public Notificador(Email email, SMS sms, WhatsApp whatsApp) {
    this.estrategias = new EnumMap<>(TipoContactoPredeterminado.class);

    estrategias.put(TipoContactoPredeterminado.MAIL, email);
    estrategias.put(TipoContactoPredeterminado.TELEFONO, sms);
    estrategias.put(TipoContactoPredeterminado.WHATSAPP, whatsApp);
  }

  public Notificacion notificarDonante(Donante donante, String mensaje) {
    java.util.Objects.requireNonNull(
        donante, "El donante es obligatorio"
    );

    TipoContactoPredeterminado canal =
        donante.getContactoPredeterminado();

    if (canal == null) {
      canal = TipoContactoPredeterminado.MAIL;
    }

    String destinatario;

    if (canal == TipoContactoPredeterminado.MAIL) {
      destinatario = donante.getMail();
    } else {
      TipoMedioContacto tipoMedio =
          canal == TipoContactoPredeterminado.WHATSAPP
              ? TipoMedioContacto.WHATSAPP
              : TipoMedioContacto.TELEFONO;

      destinatario = buscarNumero(
          donante.getMediosDeContacto(),
          tipoMedio
      );

      if (destinatario == null || destinatario.isBlank()) {
        canal = TipoContactoPredeterminado.MAIL;
        destinatario = donante.getMail();
      }
    }

    if (destinatario == null || destinatario.isBlank()) {
      throw new IllegalArgumentException(
          "El donante no tiene un contacto valido"
      );
    }

    return estrategias.get(canal).enviar(destinatario, mensaje);
  }

  public Notificacion notificarEntidadBeneficiaria(EntidadBeneficiaria entidad, String mensaje) {
    String emailRepresentante = buscarEmailRepresentante(entidad);

    if (emailRepresentante != null) {
      TipoNotificacion email = estrategias.get(TipoContactoPredeterminado.MAIL);
      return email.enviar(emailRepresentante, mensaje);
    }

    TipoNotificacion sms = estrategias.get(TipoContactoPredeterminado.TELEFONO);
    return sms.enviar(entidad.getTelefono(), mensaje);
  }

  public Notificacion notificarPorEmail(String destinatario, String mensaje) {
    TipoNotificacion email = estrategias.get(TipoContactoPredeterminado.MAIL);
    return email.enviar(destinatario, mensaje);
  }

  private String buscarNumero(List<MedioContacto> medios, TipoMedioContacto tipoMedio) {
    for (MedioContacto medio : medios) {
      if (medio.getTipo() == tipoMedio) {
        return medio.getNumero();
      }
    }

    return null;
  }

  private String buscarEmailRepresentante(EntidadBeneficiaria entidad) {
    for (Representante representante : entidad.getRepresentantes()) {
      if (representante.getEmail() != null && !representante.getEmail().isBlank()) {
        return representante.getEmail();
      }
    }

    return null;
  }
  public Notificacion notificarDonacionAsignadaBeneficiario(AsignacionDonacion asignacion) {
    EntidadBeneficiaria entidad = asignacion.getEntidad();
    String email = buscarEmailRepresentante(entidad);

    if (email == null) {
      return null;
    }

    String mensaje = "Se le asigno una donacion en base a sus necesidades. "
            + "Entidad: " + entidad.getRazonSocial() + ".";

    return notificarPorEmail(email, mensaje);
  }

  public Notificacion notificarDonacionAsignadaDonante(AsignacionDonacion asignacion) {
    Donante donante = asignacion.getDonacion().getDonante();

    String mensaje = "Tu donacion acaba de ser asignada a la entidad beneficiaria "
            + asignacion.getEntidad().getRazonSocial() + ".";

    return notificarDonante(donante, mensaje);
  }

  public void notificarEventoLogistico(AsignacionDonacion asignacion,
      org.example.dominio.donacion.EventoLogistico evento) {
    String mensajeEntidad;
    String mensajeDonante;
    switch (evento.tipo()) {
      case INICIO_TRASLADO -> {
        mensajeEntidad = "Tu envio ya esta en camino. Podes seguirlo en el mapa interactivo.";
        mensajeDonante = "Tu donacion ya esta en camino. Podes seguir el envio en el mapa interactivo.";
      }
      case RECEPCION -> {
        mensajeEntidad = "Comprobante de entrega: " + evento.fecha() + ". Camion: " + evento.patente();
        mensajeDonante = mensajeEntidad;
      }
      case NO_RECIBIDA -> {
        mensajeEntidad = "No se pudo concretar la recepcion del envio. Motivo: " + evento.motivo();
        mensajeDonante = "No se pudo concretar la entrega de tu donacion. Motivo: " + evento.motivo();
      }
      case RETORNO_DEPOSITO -> { return; }
      default -> throw new IllegalArgumentException("Evento no soportado");
    }
    notificarEntidadBeneficiaria(asignacion.getEntidad(), mensajeEntidad);
    notificarDonante(asignacion.getDonacion().getDonante(), mensajeDonante);
  }

  public Notificacion enviarPendiente(NotificacionPendiente pendiente) {
    java.util.Objects.requireNonNull(
        pendiente, "La notificacion pendiente es obligatoria"
    );

    if (!pendiente.requiereEnvio()) {
      throw new IllegalStateException(
          "La notificacion ya fue completada"
      );
    }

    TipoNotificacion estrategia =
        estrategias.get(pendiente.getCanal());

    if (estrategia == null) {
      throw new IllegalStateException(
          "No existe una estrategia para el canal indicado"
      );
    }

    return estrategia.enviar(
        pendiente.getDestinatario(),
        pendiente.getMensaje()
    );
  }

  public List<NotificacionPendiente> prepararEventoLogistico(
      AsignacionDonacion asignacion,
      EventoLogistico evento
  ) {
    String mensajeEntidad;
    String mensajeDonante;

    switch (evento.tipo()) {
      case INICIO_TRASLADO -> {
        mensajeEntidad =
            "Tu envio ya esta en camino. "
                + "Podes seguirlo en el mapa interactivo.";

        mensajeDonante =
            "Tu donacion ya esta en camino. "
                + "Podes seguir el envio en el mapa interactivo.";
      }

      case RECEPCION -> {
        mensajeEntidad =
            "Comprobante de entrega: " + evento.fecha()
                + ". Camion: " + evento.patente();

        mensajeDonante = mensajeEntidad;
      }

      case NO_RECIBIDA -> {
        mensajeEntidad =
            "No se pudo concretar la recepcion del envio. Motivo: "
                + evento.motivo();

        mensajeDonante =
            "No se pudo concretar la entrega de tu donacion. Motivo: "
                + evento.motivo();
      }

      case RETORNO_DEPOSITO -> {
        return List.of();
      }

      default -> throw new IllegalArgumentException(
          "Evento no soportado"
      );
    }

    List<NotificacionPendiente> pendientes = new ArrayList<>();

    pendientes.add(prepararParaEntidad(
        evento.idOperacion(),
        asignacion.getEntidad(),
        mensajeEntidad
    ));

    Donante donante = asignacion.getDonacion().getDonante();

    // Hay datos históricos y pruebas que no tienen donante asociado.
    if (donante != null) {
      pendientes.add(prepararParaDonante(
          evento.idOperacion(),
          donante,
          mensajeDonante
      ));
    }

    return pendientes;
  }

  private NotificacionPendiente prepararParaEntidad(
      String idOperacion,
      EntidadBeneficiaria entidad,
      String mensaje
  ) {
    String email = buscarEmailRepresentante(entidad);

    if (email != null) {
      return new NotificacionPendiente(
          idOperacion,
          TipoContactoPredeterminado.MAIL,
          email,
          mensaje
      );
    }

    return new NotificacionPendiente(
        idOperacion,
        TipoContactoPredeterminado.TELEFONO,
        entidad.getTelefono(),
        mensaje
    );
  }

  private NotificacionPendiente prepararParaDonante(
      String idOperacion,
      Donante donante,
      String mensaje
  ) {
    TipoContactoPredeterminado canal =
        donante.getContactoPredeterminado();

    if (canal == null) {
      canal = TipoContactoPredeterminado.MAIL;
    }

    String destinatario;

    if (canal == TipoContactoPredeterminado.MAIL) {
      destinatario = donante.getMail();
    } else {
      TipoMedioContacto tipoMedio =
          canal == TipoContactoPredeterminado.WHATSAPP
              ? TipoMedioContacto.WHATSAPP
              : TipoMedioContacto.TELEFONO;

      destinatario = buscarNumero(
          donante.getMediosDeContacto(), tipoMedio
      );

      if (destinatario == null || destinatario.isBlank()) {
        canal = TipoContactoPredeterminado.MAIL;
        destinatario = donante.getMail();
      }
    }

    return new NotificacionPendiente(
        idOperacion,
        canal,
        destinatario,
        mensaje
    );
  }

}
