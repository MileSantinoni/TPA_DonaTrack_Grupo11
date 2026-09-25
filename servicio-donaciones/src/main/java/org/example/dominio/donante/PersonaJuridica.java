package org.example.dominio.donante;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "personas_juridicas")
public class PersonaJuridica extends Donante {

  @Column(name = "razon_social")
  private String razonSocial;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_organizacion")
  private TipoOrganizacion tipo;
  private String rubro;

  @OneToMany(
      mappedBy = "personaJuridica",
      cascade = CascadeType.ALL,
      orphanRemoval = true
  )
  private List<Representante> representantes = new ArrayList<>();

  public PersonaJuridica(
      String mail,
      String numeroDocumento,
      TipoDocumento tipoDeDocumento,
      String razonSocial,
      TipoOrganizacion tipo,
      String rubro
  ) {
    super(mail, numeroDocumento, tipoDeDocumento);

    this.razonSocial = razonSocial;
    this.tipo = tipo;
    this.rubro = rubro;
    this.representantes = new ArrayList<>();
  }

  protected PersonaJuridica() {
  }

  public void agregarRepresentante(Representante representante) {
    java.util.Objects.requireNonNull(representante);

    representante.asociarAPersonaJuridica(this);

    if (!representantes.contains(representante)) {
      representantes.add(representante);
    }
  }

  public String getRazonSocial() {
    return razonSocial;
  }

  public TipoOrganizacion getTipo() {
    return tipo;
  }

  public String getRubro() {
    return rubro;
  }

  public List<Representante> getRepresentantes() {
    return representantes;
  }

  public void setRazonSocial(String razonSocial) {
    this.razonSocial = razonSocial;
  }

  public void setTipo(TipoOrganizacion tipo) {
    this.tipo = tipo;
  }

  public void setRubro(String rubro) {
    this.rubro = rubro;
  }
}