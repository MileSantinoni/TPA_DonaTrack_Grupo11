package org.example.Twilio;

import com.twilio.Twilio;
import com.twilio.http.TwilioRestClient;

public class TwilioConfig {

  private static TwilioRestClient cliente;

  public static synchronized TwilioRestClient cliente() {
    if (cliente == null) {
      cliente = new TwilioRestClient.Builder(
          requerida("TWILIO_ACCOUNT_SID"),
          requerida("TWILIO_AUTH_TOKEN")
      ).build();
    }
    return cliente;
  }

  public void init() {
    Twilio.setRestClient(cliente());
  }

  private static String requerida(String nombre) {
    String valor = System.getenv(nombre);

    if (valor == null || valor.isBlank()) {
      throw new IllegalStateException("Falta configurar " + nombre);
    }

    return valor.trim();
  }
}