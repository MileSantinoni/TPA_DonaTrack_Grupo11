package org.example.dominio.donante;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class Donante {

  protected String id;
  protected String mail;
  protected EstadoRegistro estadoRegistro;
  protected List<MedioContacto> mediosDeContacto;
  protected TipoContactoPredeterminado contactoPredeterminado;
  protected String numeroDocumento;
  protected TipoDocumento tipoDeDocumento;

  public Donante(String mail, String numeroDocumento, TipoDocumento tipoDeDocumento) {

    if(mail == null || mail.isBlank()) {
      throw new IllegalArgumentException(
          "El mail es obligatorio"
      );
    }
    this.id = UUID.randomUUID().toString();
    this.mail = mail;
    this.numeroDocumento = numeroDocumento;
    this.tipoDeDocumento = tipoDeDocumento;
    this.estadoRegistro = EstadoRegistro.PRIMER_ACCESO;
    this.contactoPredeterminado = TipoContactoPredeterminado.MAIL;
    this.mediosDeContacto = new ArrayList<>();
  }

  public void activar() {
    this.estadoRegistro = EstadoRegistro.ACTIVO;
  }

  public void agregarMedioContacto(MedioContacto medioContacto) {
    this.mediosDeContacto.add(medioContacto);
  }

  public void actualizarDatos(String mail, String numeroDocumento, TipoDocumento tipoDeDocumento) {
    this.mail = mail;
    this.numeroDocumento = numeroDocumento;
    this.tipoDeDocumento = tipoDeDocumento;
  }

  public void definirContactoPredeterminado(TipoContactoPredeterminado contactoPredeterminado) {
    this.contactoPredeterminado = contactoPredeterminado;
  }

  public TipoContactoPredeterminado getContactoPredeterminado() {
    return contactoPredeterminado;
  }

  public String getId() {
    return id;
  }

  public String getMail() {
    return mail;
  }

  public EstadoRegistro getEstadoRegistro() {
    return estadoRegistro;
  }

  public List<MedioContacto> getMediosDeContacto() {
    return mediosDeContacto;
  }

  public String getNumeroDocumento() {
    return numeroDocumento;
  }

  public TipoDocumento getTipoDeDocumento() {
    return tipoDeDocumento;
  }
}

