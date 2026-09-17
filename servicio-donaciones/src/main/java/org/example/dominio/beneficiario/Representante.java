package org.example.dominio.beneficiario;

import java.util.ArrayList;
import java.util.List;

public class Representante {
  private String nombre;
  private String apellido;
  private String email;

  public Representante(String nombre, String apellido, String email) {
    this.nombre = nombre;
    this.apellido = apellido;
    this.email = email;
  }
  public String getNombre() { return nombre; }
  public String getApellido() { return apellido; }
  public String getEmail() { return email; }
}