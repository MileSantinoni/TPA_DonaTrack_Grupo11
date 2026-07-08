package org.example.dominio.donacion;

import org.example.dominio.catalogo.Estado;
import org.example.dominio.catalogo.Subcategoria;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;


public class Donacion {

  private String id; //le agregue un id para la exposicion en apissss
  private String descripcionGeneral;
  private int cantidad;
  private String unidadMedida;
  private Subcategoria subcategoria;
  private LocalDate fechaVencimiento;
  private Estado estadoBien;
  private EstadoDonacionState estadoActual;
  private List<RegistroCambioEstado> historialEstados;

  public Donacion(String descripcionGeneral, int cantidad, String unidadMedida,Subcategoria subcategoria, LocalDate fechaVencimiento, Estado estadoBien) {
    this.id = UUID.randomUUID().toString(); //no se agrega en el constructor sino que es random, no hay que pasarselo
    this.descripcionGeneral = descripcionGeneral;
    this.cantidad = cantidad;
    this.unidadMedida = unidadMedida;
    this.subcategoria = subcategoria;
    this.fechaVencimiento = fechaVencimiento;
    this.estadoBien = estadoBien;
    this.estadoActual = new EstadoDonacionEnDeposito();
    this.historialEstados = new ArrayList<>();
  }


  public void cambiarEstado(EstadoDonacion nuevoEstado, String justificativo) {
    this.estadoActual.cambiarA(this, nuevoEstado, justificativo);
  }

  void aplicarCambioEstado(EstadoDonacionState nuevoEstado, String justificativo) {
    RegistroCambioEstado registro = new RegistroCambioEstado(this.estadoActual.getNombre(), nuevoEstado.getNombre(), justificativo);
    this.historialEstados.add(registro);
    this.estadoActual = nuevoEstado;
  }

  public EstadoDonacion getEstadoActual() {
    return estadoActual.getNombre();
  }

  public List<RegistroCambioEstado> getHistorialEstados() {
    return historialEstados;
  }

  public String getDescripcionGeneral() {
    return descripcionGeneral;
  }

  public int getCantidad(){
    return cantidad;
  }

  public Subcategoria getSubcategoria() {
    return subcategoria;
  }

  public LocalDate getFechaVencimiento(){
    return fechaVencimiento;
  }

  public Estado getEstadoBien(){
    return estadoBien;
  }


  public String getId() {
    return id;
  }

}
