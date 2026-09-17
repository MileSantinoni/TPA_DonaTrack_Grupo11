package org.example.api.donaciones.dto;

import org.example.dominio.donante.TipoDocumento;
import org.example.dominio.donante.TipoOrganizacion;

public class PersonaJuridicaRequest {
  private String mail;
  private String numeroDocumento;
  private TipoDocumento tipoDeDocumento;
  private String razonSocial;
  private TipoOrganizacion tipo;
  private String rubro;


  public String getMail() { return mail; }
  public void setMail(String mail) { this.mail = mail; }
  public String getNumeroDocumento() { return numeroDocumento; }
  public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
  public TipoDocumento getTipoDeDocumento() { return tipoDeDocumento; }
  public void setTipoDeDocumento(TipoDocumento tipoDeDocumento) { this.tipoDeDocumento = tipoDeDocumento; }
  public String getRazonSocial() { return razonSocial; }
  public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
  public TipoOrganizacion getTipo() { return tipo; }
  public void setTipo(TipoOrganizacion tipo) { this.tipo = tipo; }
  public String getRubro() { return rubro; }
  public void setRubro(String rubro) { this.rubro = rubro; }
}
