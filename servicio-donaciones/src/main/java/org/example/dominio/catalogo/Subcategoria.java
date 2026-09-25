package org.example.dominio.catalogo;

import javax.persistence.*;
@Entity
@Table(name = "subcategorias")
public class Subcategoria {
  @Id
  private String id;

  private String nombre;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_atributo")
  private TipoAtributo tipo; // El atributo es de la clase enum TipoAtributo

  @ManyToOne
  @JoinColumn(name = "categoria_id")
  private Categoria categoria;

  protected Subcategoria() {}

  // Constructor
  public Subcategoria(String id, String nombre, TipoAtributo tipo) {
    this.id = id;
    this.nombre = nombre;
    this.tipo = tipo;
  }

  public void setCategoria(Categoria nuevaCategoria) {
    if (this.categoria == nuevaCategoria) {
      return;
    }

    Categoria anterior = this.categoria;
    this.categoria = nuevaCategoria;

    if (anterior != null) {
      anterior.quitarSubcategoria(this);
    }

    if (nuevaCategoria != null) {
      nuevaCategoria.incorporarSubcategoria(this);
    }
  }

  // Getters
  public String getId() { return id; }
  public String getNombre() { return nombre; }
  public TipoAtributo getTipo() { return tipo; }
  public Categoria getCategoria() { return categoria; }
}
