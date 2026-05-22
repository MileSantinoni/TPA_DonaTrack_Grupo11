package org.example.dominio.donante;

import java.util.UUID;

public class Administrador {

  private String id;
  private String nombre;
  private String mail;

  public Administrador(String nombre, String mail) {
    this.id = UUID.randomUUID().toString();
    this.nombre = nombre;
    this.mail = mail;
  }

  public void activarDonante(Donante donante) {
    donante.activar();
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