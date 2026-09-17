package org.example.dominio.donacion;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Necesidad;

import java.time.LocalDate;
import java.util.UUID;

public class AsignacionDonacion {
  private String id;
  private Donacion donacion;
  private EntidadBeneficiaria entidad;
  private Necesidad necesidad;
  private LocalDate fechaRecepcion;

  public AsignacionDonacion(Donacion donacion, EntidadBeneficiaria entidad, Necesidad necesidad, LocalDate fechaRecepcion) {
    this.id = UUID.randomUUID().toString();
    this.donacion = donacion;
    this.entidad = entidad;
    this.necesidad = necesidad;
    this.fechaRecepcion = fechaRecepcion;
  }

  public String getId() {
    return id;
  }

  public Donacion getDonacion() {
    return donacion;
  }

  public EntidadBeneficiaria getEntidad() {
    return entidad;
  }

  public Necesidad getNecesidad() {
    return necesidad;
  }

  public LocalDate getFechaRecepcion() {
    return fechaRecepcion;
  }
}
