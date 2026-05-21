package org.example.dominio.donante;

import java.util.ArrayList;
import java.util.List;

public class PersonaJuridica extends Donante {

  private String razonSocial;
  private TipoOrganizacion tipo;
  private String rubro;
  private List<Representante> representantes;

  public PersonaJuridica(
      String id,
      String mail,
      String numeroDocumento,
      TipoDocumento tipoDeDocumento,
      String razonSocial,
      TipoOrganizacion tipo,
      String rubro
  ) {
    super(id, mail, numeroDocumento, tipoDeDocumento);

    this.razonSocial = razonSocial;
    this.tipo = tipo;
    this.rubro = rubro;
    this.representantes = new ArrayList<>();
  }

  public void agregarRepresentante(Representante representante) {
    this.representantes.add(representante);
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