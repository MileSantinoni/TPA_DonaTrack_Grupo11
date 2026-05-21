package org.example.dominio.beneficiario;

import java.time.LocalDate;
import org.example.dominio.catalogo.Subcategoria;

// Clase Abstracta Necesidad
public abstract class Necesidad {
  protected String descripcion;
  protected int cantidadObjetivo;
  protected int cantidadCubierta;
  protected Subcategoria subcategoria;

  public Necesidad(String descripcion, int cantidadObjetivo, Subcategoria subcategoria) {
    this.descripcion = descripcion;
    this.cantidadObjetivo = cantidadObjetivo;
    this.subcategoria = subcategoria;
    this.cantidadCubierta = 0; // Inicia en 0
  }

  // Metodo para sumar donaciones parciales
  public void registrarDonacion(int cantidad) {
    this.cantidadCubierta += cantidad;
  }

  // Metodo abstracto que cada tipo de necesidad debe implementar
  public abstract boolean estaSatisfecha();


}
