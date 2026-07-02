package org.example.dominio.notificacion;

import org.example.dominio.donante.Donante;
import org.example.dominio.donante.MedioContacto;
import org.example.dominio.donante.TipoContactoPredeterminado;
import org.example.dominio.donante.TipoMedioContacto;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class Notificador {

  private final Map<TipoContactoPredeterminado, TipoNotificacion> estrategias;

  public Notificador() {
    this.estrategias = new EnumMap<>(TipoContactoPredeterminado.class);

    estrategias.put(TipoContactoPredeterminado.MAIL, new Email());
    estrategias.put(TipoContactoPredeterminado.TELEFONO, new SMS());
    estrategias.put(TipoContactoPredeterminado.WHATSAPP, new WhatsApp());
  }

  public Notificacion notificarDonante(Donante donante, String mensaje) {
    String destinatario = resolverDestinatario(donante);

    TipoNotificacion tipoNotificacion =
        estrategias.get(donante.getContactoPredeterminado());

    return tipoNotificacion.enviar(destinatario, mensaje);
  }

  private String resolverDestinatario(Donante donante) {
    TipoContactoPredeterminado tipo = donante.getContactoPredeterminado();

    if (tipo == TipoContactoPredeterminado.MAIL) {
      return donante.getMail();
    }

    TipoMedioContacto tipoMedio;

    if (tipo == TipoContactoPredeterminado.WHATSAPP) {
      tipoMedio = TipoMedioContacto.WHATSAPP;
    } else {
      tipoMedio = TipoMedioContacto.TELEFONO;
    }

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

  public Notificacion notificarPorEmail(String destinatario, String mensaje) {
    TipoNotificacion email = estrategias.get(TipoContactoPredeterminado.MAIL);
    return email.enviar(destinatario, mensaje);
  }
}