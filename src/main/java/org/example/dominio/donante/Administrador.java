package org.example.dominio.donante;

import java.util.UUID;
import org.example.dominio.notificacion.MedioNotificacion;
import org.example.dominio.notificacion.Notificador;

public class Administrador {

  private String id;
  private String nombre;
  private String mail;

  public Administrador(String nombre, String mail) {
    this.id = UUID.randomUUID().toString();
    this.nombre = nombre;
    this.mail = mail;
  }


  public void activarDonante(Donante donante, Notificador notificador) {
    if (donante.getEstadoRegistro() == EstadoRegistro.PRIMER_ACCESO) {
      notificador.enviar(
          donante.getMail(),
          "Bienvenido/a a DonaTrack. Ya podés acceder por primera vez.",
          MedioNotificacion.EMAIL
      );

      donante.activar();
    }
  }


  public String getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public String getMail() {
    return mail;
  }
}