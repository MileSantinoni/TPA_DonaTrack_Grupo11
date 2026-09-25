package org.example.dominio.donacion;

import org.example.dominio.catalogo.Bien;
import org.example.dominio.donante.Donante;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;

@Entity
@Table(name = "registros_donacion")
public class RegistroDonacion {
  @Id
  private String id;
  private String descripcionGeneral;
  @ManyToOne
  @JoinColumn(name = "donante_id")
  private Donante donante;

  @Column(name = "fecha_de_registro")
  private LocalDate fechaDeRegistro;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "registro_donacion_id")
  private List<Bien> listaBienes = new ArrayList<>();

  protected RegistroDonacion() {}

  public RegistroDonacion(String id, String descripcionGeneral, Donante donante){
    this.id = id;
    this.descripcionGeneral = descripcionGeneral;
    this.donante = donante;
    this.fechaDeRegistro = LocalDate.now();
    this.listaBienes = new ArrayList<>();
  }

  public void agregarBien(Bien bien) {
    this.listaBienes.add(bien);
  }

  public List<Donacion> segmentar() {
    List<Donacion> donaciones = new ArrayList<>();

    for (Bien bien : listaBienes) {
      Donacion donacion = new Donacion(
          bien.getDescripcion(),
          bien.getCantidad(),
          bien.getUnidadMedida(),
          bien.getSubcategoria(),
          this.fechaDeRegistro,
          bien.getFechaVencimiento(),
          bien.getEstado(),
          donante
      );

      donaciones.add(donacion);
    }

    return donaciones;
  }


  public String getId() {
    return id;
  }
  public Donante getDonante() {
    return donante;
  }

  public LocalDate getFechaDeRegistro() {
    return fechaDeRegistro;
  }

  public String getDescripcionGeneral() {
    return descripcionGeneral;
  }

  public List<Bien> getListaBienes() {
    return List.copyOf(listaBienes);
  }
}
