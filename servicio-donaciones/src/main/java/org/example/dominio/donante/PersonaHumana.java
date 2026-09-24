package org.example.dominio.donante;

import javax.persistence.*;

@Entity
@Table(name = "personas_humanas")
public class PersonaHumana extends Donante {

  private String nombre;
  private String apellido;
  private int edad;

  @Enumerated(EnumType.STRING)
  private Genero genero;
  private String direccion;

  protected PersonaHumana() {}
  public PersonaHumana(
      String mail,
      String numeroDocumento,
      TipoDocumento tipoDeDocumento,
      String nombre,
      String apellido,
      int edad,
      Genero genero,
      String direccion
  ) {
    super(mail, numeroDocumento, tipoDeDocumento);

    this.nombre = nombre;
    this.apellido = apellido;
    this.edad = edad;
    this.genero = genero;
    this.direccion = direccion;
  }

  public String getNombre() {
    return nombre;
  }

  public String getApellido() {
    return apellido;
  }

  public int getEdad() {
    return edad;
  }

  public Genero getGenero() {
    return genero;
  }

  public String getDireccion() {
    return direccion;
  }

  public void setNombre(String nombre) {
    this.nombre = nombre;
  }

  public void setApellido(String apellido) {
    this.apellido = apellido;
  }

  public void setEdad(int edad) {
    this.edad = edad;
  }

  public void setGenero(Genero genero) {
    this.genero = genero;
  }

  public void setDireccion(String direccion) {
    this.direccion = direccion;
  }
}