package org.example.dominio.donacion;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Necesidad;

import java.time.LocalDate;
import java.util.UUID;
import javax.persistence.*;

@Entity
@Table(name = "asignaciones_donacion")
public class AsignacionDonacion {

  @Id
  private UUID id;

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "donacion_id")
  private Donacion donacion;

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "entidad_beneficiaria_id")
  private EntidadBeneficiaria entidad;

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "necesidad_id")
  private Necesidad necesidad;

  @Column(name = "fecha_recepcion")
  private LocalDate fechaRecepcion;

  protected AsignacionDonacion() {
  }

  public AsignacionDonacion(Donacion donacion, EntidadBeneficiaria entidad,
                            Necesidad necesidad, LocalDate fechaRecepcion) {
    this.id = UUID.randomUUID();
    this.donacion = donacion;
    this.entidad = entidad;
    this.necesidad = necesidad;
    this.fechaRecepcion = fechaRecepcion;
  }

  public void registrarRecepcion(LocalDate fecha) {
    java.util.Objects.requireNonNull(fecha);

    if (fechaRecepcion != null
        && !fechaRecepcion.equals(fecha)) {
      throw new IllegalStateException(
          "La asignación ya tiene otra fecha de recepción."
      );
    }

    if (necesidad != null) {
      if (!necesidad.compatibleCon(donacion)) {
        throw new IllegalStateException(
            "La necesidad asignada no coincide con "
                + "la subcategoría y unidad de la donación."
        );
      }

      necesidad.registrarRecepcion(
          getIdAsString(),
          donacion.getCantidad(),
          fecha
      );
    }

    fechaRecepcion = fecha;
  }

  public UUID getId() {
    return id;
  }

  public String getIdAsString() {
    return id != null ? id.toString() : null;
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