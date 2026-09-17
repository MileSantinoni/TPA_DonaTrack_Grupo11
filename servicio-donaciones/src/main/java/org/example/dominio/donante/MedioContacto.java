package org.example.dominio.donante;

public class MedioContacto {

  private TipoMedioContacto tipo;
  private String numero;

  public MedioContacto(TipoMedioContacto tipo, String numero) {
    this.tipo = tipo;
    this.numero = numero;
  }

  public TipoMedioContacto getTipo() {
    return tipo;
  }

  public String getNumero() {
    return numero;
  }

  public void setTipo(TipoMedioContacto tipo) {
    this.tipo = tipo;
  }

  public void setNumero(String numero) {
    this.numero = numero;
  }
}