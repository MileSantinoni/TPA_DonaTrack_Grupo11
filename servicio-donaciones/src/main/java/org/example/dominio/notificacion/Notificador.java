package org.example.dominio.notificacion;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Representante;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.MedioContacto;
import org.example.dominio.donante.TipoContactoPredeterminado;
import org.example.dominio.donante.TipoMedioContacto;
import org.example.service.NotificacionesService;

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
}
