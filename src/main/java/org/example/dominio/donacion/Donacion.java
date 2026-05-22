package org.example.dominio.donacion;

import org.example.dominio.catalogo.Estado;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.donante.Donante;

import java.time.LocalDate;
import java.util.List;


public class Donacion {

  private String descripcionGeneral;
  private int cantidad;
  private String unidadMedida;
  private Subcategoria subcategoria;
  private LocalDate fechaVencimiento;
  private Estado estadoBien;
  private EstadoDonacion estadoActual;
  private List<RegistroCambioEstado> historialEstados;

  public Donacion(String descripcionGeneral, int cantidad, String unidadMedida,Subcategoria subcategoria, LocalDate fechaVencimiento, Estado estadoBien) {
    this.descripcionGeneral = descripcionGeneral;
    this.cantidad = cantidad;
    this.unidadMedida = unidadMedida;
    this.subcategoria = subcategoria;
    this.fechaVencimiento = fechaVencimiento;
    this.estadoBien = estadoBien;
    this.estadoActual = EstadoDonacion.EN_DEPOSITO;
  }
  public void cambiarEstado(EstadoDonacion nuevoEstado, String justificativo) {
    RegistroCambioEstado registro = new RegistroCambioEstado(this.estadoActual, nuevoEstado, justificativo);
    this.historialEstados.add(registro);
    this.estadoActual = nuevoEstado;
  }

  public EstadoDonacion getEstadoActual() {
    return estadoActual;
  }

  public List<RegistroCambioEstado> getHistorialEstados() {
    return historialEstados;
  }
}
