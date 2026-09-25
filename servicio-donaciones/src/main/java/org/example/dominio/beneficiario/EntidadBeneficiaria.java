package org.example.dominio.beneficiario;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "entidades_beneficiarias")
public class EntidadBeneficiaria {

  @Id
  private UUID id;

  @Column(name = "razon_social")
  private String razonSocial;

  private String direccion;

  private String telefono;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "entidad_beneficiaria_id")
  private List<Representante> representantes = new ArrayList<>();

  @OneToMany(mappedBy = "entidadBeneficiaria", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<Necesidad> necesidades = new ArrayList<>();

  protected EntidadBeneficiaria() {
  }

  public EntidadBeneficiaria(String razonSocial, String direccion, String telefono) {
    this.id = UUID.randomUUID();
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
    this.representantes = new ArrayList<>();
    this.necesidades = new ArrayList<>();
  }

  public void agregarRepresentante(Representante representante) {
    this.representantes.add(representante);
  }

  public void registrarNecesidad(Necesidad necesidad) {
    this.necesidades.add(necesidad);
    necesidad.setEntidadBeneficiaria(this);
  }

  // Getters y Setters
  public UUID getId() {
    return id;
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public String getDireccion() {
    return direccion;
  }

  public String getTelefono() {
    return telefono;
  }

  public List<Representante> getRepresentantes() {
    return representantes;
  }

  public List<Necesidad> getNecesidades() {
    return necesidades;
  }

  public void setRazonSocial(String razonSocial) {
    this.razonSocial = razonSocial;
  }

  public void setDireccion(String direccion) {
    this.direccion = direccion;
  }

  public void setTelefono(String telefono) {
    this.telefono = telefono;
  }

  public String getIdAsString() {
    return id != null ? id.toString() : null;
  }
}