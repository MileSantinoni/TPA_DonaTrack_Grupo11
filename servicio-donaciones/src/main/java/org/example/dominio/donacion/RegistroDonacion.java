package org.example.dominio.donacion;

import org.example.dominio.catalogo.Bien;
import org.example.dominio.donante.Donante;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.example.dominio.catalogo.Estado;

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

  private record ClaveSegmentacion(
      String idSubcategoria,
      String unidadMedida,
      LocalDate fechaVencimiento,
      Estado estadoBien
  ) {
  }

  public List<Donacion> segmentar() {
    if (listaBienes.isEmpty()) {
      throw new IllegalArgumentException(
          "El registro debe contener al menos un bien"
      );
    }

    Map<ClaveSegmentacion, List<Bien>> grupos = new LinkedHashMap<>();

    for (Bien bien : listaBienes) {
      if (bien == null) {
        throw new IllegalArgumentException("El bien es obligatorio");
      }

      if (bien.getSubcategoria() == null
          || bien.getSubcategoria().getId() == null
          || bien.getSubcategoria().getId().isBlank()) {
        throw new IllegalArgumentException(
            "Cada bien debe tener una subcategoria"
        );
      }

      if (bien.getDescripcion() == null
          || bien.getDescripcion().isBlank()) {
        throw new IllegalArgumentException(
            "La descripcion del bien es obligatoria"
        );
      }

      if (bien.getCantidad() <= 0) {
        throw new IllegalArgumentException(
            "La cantidad de cada bien debe ser mayor a cero"
        );
      }

      if (bien.getUnidadMedida() == null
          || bien.getUnidadMedida().isBlank()) {
        throw new IllegalArgumentException(
            "La unidad de medida es obligatoria"
        );
      }

      String unidad = bien.getUnidadMedida()
          .trim()
          .toUpperCase(Locale.ROOT);

      ClaveSegmentacion clave = new ClaveSegmentacion(
          bien.getSubcategoria().getId(),
          unidad,
          bien.getFechaVencimiento(),
          bien.getEstado()
      );

      grupos.computeIfAbsent(clave, ignorada -> new ArrayList<>())
          .add(bien);
    }

    List<Donacion> resultado = new ArrayList<>();

    for (var entrada : grupos.entrySet()) {
      ClaveSegmentacion clave = entrada.getKey();
      List<Bien> bienes = entrada.getValue();
      Bien primero = bienes.get(0);

      int cantidadTotal = 0;

      for (Bien bien : bienes) {
        try {
          cantidadTotal = Math.addExact(
              cantidadTotal, bien.getCantidad()
          );
        } catch (ArithmeticException e) {
          throw new IllegalArgumentException(
              "La cantidad total supera el limite permitido", e
          );
        }
      }

      // Las descripciones individuales permanecen en los bienes
      // originales del registro.
      String descripcion = bienes.size() == 1
          ? primero.getDescripcion()
          : primero.getSubcategoria().getNombre();

      resultado.add(new Donacion(
          descripcion,
          cantidadTotal,
          clave.unidadMedida(),
          primero.getSubcategoria(),
          fechaDeRegistro,
          clave.fechaVencimiento(),
          clave.estadoBien(),
          donante
      ));
    }

    return resultado;
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
