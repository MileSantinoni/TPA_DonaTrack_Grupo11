package org.example.dominio.donacion;

import org.example.dominio.catalogo.Bien;
import org.example.dominio.donante.Donante;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class RegistroDonacion {
  private String id;
  private String descripcionGeneral;
  private Donante donante;
  private LocalDate fechaDeRegistro;
  private List<Bien> listaBienes;

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
          (int) bien.getCantidad(),
          bien.getUnidadMedida()
      );
      /*    bien.getSubcategoria(),
          bien.getFechaVencimiento(),
          bien.getCondicion()*/


      donaciones.add(donacion);
    }

    return donaciones;
  }


}
