package org.example.dominio.donante;

import javax.persistence.*;
import org.example.dominio.beneficiario.EntidadBeneficiaria;

@Entity(name = "RepresentanteDonante")
@Table(name = "representantes_donantes")
public class Representante {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "entidad_beneficiaria_id")
  private EntidadBeneficiaria entidadBeneficiaria;

  @ManyToOne
  @JoinColumn(name = "persona_juridica_id")
  private PersonaJuridica personaJuridica;

  private String nombre;
  private String apellido;
  private String email;

  protected Representante() {
  }

  // Conservá debajo el constructor público, getters y setters actuales.
// este para personas juridicas
  public Representante(String nombre, String apellido, String email) {
    this.nombre = nombre;
    this.apellido = apellido;
    this.email = email;
//    this.personaJuridica = personaJuridica;
  }

  // Constructor para Entidad Beneficiaria
//  public Representante(String nombre, String apellido, String email, String telefono, EntidadBeneficiaria entidadBeneficiaria) {
//    this.nombre = nombre;
//    this.apellido = apellido;
//    this.email = email;
//    this.entidadBeneficiaria = entidadBeneficiaria;
//  }

  public void asociarAEntidad(EntidadBeneficiaria entidad) {
    java.util.Objects.requireNonNull(entidad);

    if (personaJuridica != null) {
      throw new IllegalStateException(
          "El representante ya pertenece a una persona juridica"
      );
    }

    if (entidadBeneficiaria != null && entidadBeneficiaria != entidad) {
      throw new IllegalStateException(
          "El representante ya pertenece a otra entidad"
      );
    }

    this.entidadBeneficiaria = entidad;
  }

  public void asociarAPersonaJuridica(PersonaJuridica persona) {
    java.util.Objects.requireNonNull(persona);

    if (entidadBeneficiaria != null) {
      throw new IllegalStateException(
          "El representante ya pertenece a una entidad beneficiaria"
      );
    }

    if (personaJuridica != null && personaJuridica != persona) {
      throw new IllegalStateException(
          "El representante ya pertenece a otra persona juridica"
      );
    }

    this.personaJuridica = persona;
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

  public PersonaJuridica getPersonaJuridica() {
    return personaJuridica;
  }

  public void setPersonaJuridica(PersonaJuridica personaJuridica) {
    this.personaJuridica = personaJuridica;
  }
}