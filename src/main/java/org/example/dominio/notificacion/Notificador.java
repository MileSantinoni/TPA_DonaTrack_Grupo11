package org.example.dominio.notificacion;

import org.example.dominio.donante.Donante;
import org.example.dominio.donante.MedioContacto;
import org.example.dominio.donante.TipoContactoPredeterminado;
import org.example.dominio.donante.TipoMedioContacto;
import java.util.List;

public class Notificador {

  // Envío directo dado destinatario, mensaje y medio explícitos
  public Notificacion enviar(String destinatario, String mensaje, MedioNotificacion medio) {
    Notificacion notificacion = new Notificacion(destinatario, mensaje, medio);

    try {
      simularEnvio(medio, destinatario, mensaje);
      notificacion.marcarComoCompletada();
    } catch (Exception e) {
      notificacion.marcarComoFallida();
    }

    return notificacion;
  }

  // Envío resolviendo automáticamente el contacto predeterminado del donante
  public Notificacion notificarDonante(Donante donante, String mensaje) {
    String destinatario = resolverDestinatario(donante);
    MedioNotificacion medio = resolverMedio(donante.getContactoPredeterminado());
    return enviar(destinatario, mensaje, medio);
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

  private MedioNotificacion resolverMedio(TipoContactoPredeterminado tipo) {
    switch (tipo) {
      case WHATSAPP: return MedioNotificacion.WHATSAPP;
      case TELEFONO: return MedioNotificacion.SMS;
      default:       return MedioNotificacion.EMAIL;
    }
  }

  private void simularEnvio(MedioNotificacion medio, String destinatario, String mensaje) {
    System.out.println("[" + medio + " simulado] Para: " + destinatario + " | Mensaje: " + mensaje);
  }
}
