package org.example.dominio.beneficiario;

import java.util.UUID;
import org.example.dominio.catalogo.Subcategoria;

// Clase Abstracta Necesidad
public abstract class Necesidad {
  protected String id;
  protected String descripcion;
  protected int cantidadObjetivo;
  protected int cantidadCubierta;
  protected Subcategoria subcategoria;

  public Necesidad(String descripcion, int cantidadObjetivo, Subcategoria subcategoria) {
    this.id = UUID.randomUUID().toString();
    this.descripcion = descripcion;
    this.cantidadObjetivo = cantidadObjetivo;
    this.subcategoria = subcategoria;
    this.cantidadCubierta = 0; // Inicia en 0
  }


  public void registrarDonacion(int cantidad) {
    this.cantidadCubierta += cantidad;
  }
  public abstract boolean estaSatisfecha();

  public String getId() {
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
