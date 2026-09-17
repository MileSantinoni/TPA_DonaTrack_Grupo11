package org.example.api.donaciones.dto;

import org.example.dominio.donante.Genero;
import org.example.dominio.donante.TipoDocumento;

public class PersonaHumanaRequest {
  private String mail;
  private String numeroDocumento;
  private TipoDocumento tipoDeDocumento;
  private String nombre;
  private String apellido;
  private int edad;
  private Genero genero;
  private String direccion;


  public String getMail() { return mail; }
  public void setMail(String mail) { this.mail = mail; }
  public String getNumeroDocumento() { return numeroDocumento; }
  public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
  public TipoDocumento getTipoDeDocumento() { return tipoDeDocumento; }
  public void setTipoDeDocumento(TipoDocumento tipoDeDocumento) { this.tipoDeDocumento = tipoDeDocumento; }
  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }
  public String getApellido() { return apellido; }
  public void setApellido(String apellido) { this.apellido = apellido; }
  public int getEdad() { return edad; }
  public void setEdad(int edad) { this.edad = edad; }
  public Genero getGenero() { return genero; }
  public void setGenero(Genero genero) { this.genero = genero; }
  public String getDireccion() { return direccion; }
  public void setDireccion(String direccion) { this.direccion = direccion; }
}
