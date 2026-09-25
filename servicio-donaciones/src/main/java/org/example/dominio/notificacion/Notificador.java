package org.example.dominio.notificacion;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.MedioContacto;
import org.example.dominio.donante.TipoContactoPredeterminado;
import org.example.dominio.donante.TipoMedioContacto;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donante.Representante;

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
    String destinatario = resolverDestinatario(donante);

    TipoNotificacion tipoNotificacion =
        estrategias.get(donante.getContactoPredeterminado());

    return tipoNotificacion.enviar(destinatario, mensaje);
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

  // este hay que borrarlo quedo olvidado je
  private String resolverDestinatario(Donante donante) {
    TipoContactoPredeterminado tipo = donante.getContactoPredeterminado();

    if (tipo == TipoContactoPredeterminado.MAIL) {
      return donante.getMail();
    }

    TipoMedioContacto tipoMedio =
        tipo == TipoContactoPredeterminado.WHATSAPP
            ? TipoMedioContacto.WHATSAPP
            : TipoMedioContacto.TELEFONO;

    String numero = buscarNumero(donante.getMediosDeContacto(), tipoMedio);

    if (numero != null) {
      return numero;
    }

    return donante.getMail();
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

}
