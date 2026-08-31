package org.example.dominio.donacion;

import org.example.dominio.catalogo.Estado;
import org.example.dominio.catalogo.Subcategoria;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.example.dominio.donante.Donante;


public class Donacion {

  private String id; //le agregue un id para la exposicion en apissss
  private String descripcionGeneral;
  private int cantidad;
  private String unidadMedida;
  private Subcategoria subcategoria;
  private LocalDate fechaDeRegistro;
  private LocalDate fechaVencimiento;
  private Estado estadoBien;
  private EstadoDonacionState estadoActual;
  private List<RegistroCambioEstado> historialEstados;
  private Donante donante;

  public Donacion(
      String descripcionGeneral,
      int cantidad,
      String unidadMedida,
      Subcategoria subcategoria,
      LocalDate fechaVencimiento,
      Estado estadoBien,
      Donante donante
  ) {
    this(
        descripcionGeneral,
        cantidad,
        unidadMedida,
        subcategoria,
        LocalDate.now(),
        fechaVencimiento,
        estadoBien,
        donante
    );
  }

  public Donacion(
      String descripcionGeneral,
      int cantidad,
      String unidadMedida,
      Subcategoria subcategoria,
      LocalDate fechaDeRegistro,
      LocalDate fechaVencimiento,
      Estado estadoBien,
      Donante donante
  ) {
    this.id = UUID.randomUUID().toString();
    this.descripcionGeneral = descripcionGeneral;
    this.cantidad = cantidad;
    this.unidadMedida = unidadMedida;
    this.subcategoria = subcategoria;
    this.fechaDeRegistro = fechaDeRegistro;
    this.fechaVencimiento = fechaVencimiento;
    this.estadoBien = estadoBien;
    this.estadoActual = new EstadoDonacionEnDeposito();
    this.historialEstados = new ArrayList<>();
    this.donante = donante;
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

  public LocalDate getFechaDeRegistro() {
    return fechaDeRegistro;
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

  public Donante getDonante() {
    return donante;
  }

  public AsignacionDonacion asignarA(org.example.dominio.beneficiario.EntidadBeneficiaria entidad) {
    if (this.getEstadoActual() != EstadoDonacion.EN_DEPOSITO) {
      throw new IllegalStateException("La donacion no esta en deposito");
    }
    org.example.dominio.beneficiario.Necesidad necesidad =
        entidad.getNecesidades().isEmpty() ? null : entidad.getNecesidades().get(0);
    AsignacionDonacion asignacion =
        new AsignacionDonacion(this, entidad, necesidad, LocalDate.now());
    cambiarEstado(
        EstadoDonacion.ASIGNACION_REALIZADA,
        "Asignada a la entidad beneficiaria " + entidad.getRazonSocial()
    );
    return asignacion;
  }

}
