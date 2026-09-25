package org.example.dominio.catalogo;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "categorias")
public class Categoria {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String nombre;

  @OneToMany(mappedBy = "categoria", cascade = CascadeType.ALL)
  private List<Subcategoria> subcategorias = new ArrayList<>();

  protected Categoria() {}
  public Categoria(String nombre) {
    this.nombre = nombre;
    this.subcategorias = new ArrayList<>();
  }

  public void agregarSubcategoria(Subcategoria subcategoria) {
    java.util.Objects.requireNonNull(subcategoria);
    subcategoria.setCategoria(this);
  }

  void incorporarSubcategoria(Subcategoria subcategoria) {
    if (!subcategorias.contains(subcategoria)) {
      subcategorias.add(subcategoria);
    }
  }

  void quitarSubcategoria(Subcategoria subcategoria) {
    subcategorias.remove(subcategoria);
  }


  public Long getId() { return id; }
  public String getNombre() { return nombre; }
  public List<Subcategoria> getSubcategorias() {
    return List.copyOf(subcategorias);
  }
}
