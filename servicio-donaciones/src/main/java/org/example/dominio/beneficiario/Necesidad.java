package org.example.dominio.beneficiario;

import java.util.UUID;
import org.example.dominio.catalogo.Subcategoria;
import javax.persistence.*;

// Clase Abstracta Necesidad

@Entity
@Table(name = "necesidades")
@Inheritance(strategy = InheritanceType.JOINED)

public abstract class Necesidad {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  protected String descripcion;

  @Column(name = "cantidad_objetivo")
  protected int cantidadObjetivo;

  @Column(name = "cantidad_cubierta")
  protected int cantidadCubierta;

  @ManyToOne
  @JoinColumn(name = "subcategoria_id")
  protected Subcategoria subcategoria;

  //TODO esto hay que revisarlo, quizá estamos acoplando al pp
  @ManyToOne
  @JoinColumn(name = "entidad_beneficiaria_id")
  protected EntidadBeneficiaria entidadBeneficiaria;

  protected Necesidad() {}

  public Necesidad(String descripcion, int cantidadObjetivo, Subcategoria subcategoria) {
//    this.id = id;
    this.descripcion = descripcion;
    this.cantidadObjetivo = cantidadObjetivo;
    this.subcategoria = subcategoria;
    this.cantidadCubierta = 0; // Inicia en 0
  }


  public void registrarDonacion(int cantidad) {
    this.cantidadCubierta += cantidad;
  }
  public abstract boolean estaSatisfecha();

  public Long getId() {
    return id;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public int getCantidadObjetivo() {
    return cantidadObjetivo;
  }

  public int getCantidadCubierta() {
    return cantidadCubierta;
  }

  public int getCantidadPendiente() {
    return cantidadObjetivo - cantidadCubierta;
  }

  public Subcategoria getSubcategoria() {
    return subcategoria;
  }
}
