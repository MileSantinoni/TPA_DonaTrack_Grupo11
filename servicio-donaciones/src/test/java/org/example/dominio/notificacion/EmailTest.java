package org.example.dominio.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


@Disabled("Test manual: envia mail real")
public class EmailTest {

  private Email email;

  @Disabled("Test manual: envía mail real")
  @Test
  void enviaMailReal() {
    Notificacion notificacion = email.enviar(
        "gianlucarone@gmail.com",
        "Prueba real DonaTrack"
    );

    assertEquals(EstadoNotificacion.COMPLETADA, notificacion.getEstado());
  }
}
