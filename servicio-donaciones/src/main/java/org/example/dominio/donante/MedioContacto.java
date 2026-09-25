package org.example.dominio.donante;

import javax.persistence.*;

@Entity
@Table(name = "medios_contacto")
public class MedioContacto {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_medio")
  private TipoMedioContacto tipo;

  private String numero;

  @ManyToOne
  @JoinColumn(name = "donante_id")
  private Donante donante;

  protected MedioContacto() {}

  public MedioContacto(TipoMedioContacto tipo, String numero) {
    this.tipo = tipo;
    this.numero = numero;
  }

  public void setDonante(Donante donante) {
    java.util.Objects.requireNonNull(donante);

    if (this.donante != null && this.donante != donante) {
      throw new IllegalStateException(
          "El medio de contacto ya pertenece a otro donante"
      );
    }

    this.donante = donante;
  }

  public Long getId() { return id; }
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