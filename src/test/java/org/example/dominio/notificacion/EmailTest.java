package org.example.dominio.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Test manual: envia mail real")
public class EmailTest {

  @Autowired
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
