package org.example.dominio.donante;

import javax.persistence.*;

@Entity(name = "RepresentanteDonante")
@Table(name = "representantes_donantes")
public class Representante {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String nombre;
  private String apellido;
  private String email;

  protected Representante() {
  }

  // Conservá debajo el constructor público, getters y setters actuales.

  public Representante(String nombre, String apellido, String email) {
    this.nombre = nombre;
    this.apellido = apellido;
    this.email = email;
  }

  public String getNombre() {
    return nombre;
  }

  public String getApellido() {
    return apellido;
  }

  public String getEmail() {
    return email;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public void setApellido(String apellido) {
    this.apellido = apellido;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Long getId() {
    return id;
  }
}