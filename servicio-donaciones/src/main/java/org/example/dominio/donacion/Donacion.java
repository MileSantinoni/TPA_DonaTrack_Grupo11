package org.example.dominio.donacion;

import org.example.dominio.catalogo.Estado;
import org.example.dominio.catalogo.Subcategoria;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.example.dominio.donante.Donante;
import javax.persistence.*;

@Entity
@Table(name = "donaciones")
public class Donacion {

  @Id
  @GeneratedValue
  private UUID id; //le agregue un id para la exposicion en apissss

  @Column(name = "descripcion_general")
  private String descripcionGeneral;

  private int cantidad;

  @Column(name = "unidad_medida")
  private String unidadMedida;

  @ManyToOne
  @JoinColumn(name = "subcategoria_id")
  private Subcategoria subcategoria;

  @Column(name = "fecha_de_registro")
  private LocalDate fechaDeRegistro;

  @Column(name = "fecha_vencimiento")
  private LocalDate fechaVencimiento;

//  @Enumerated(EnumType.STRING)
  @Column(name = "estado_bien")
  private Estado estadoBien;

  @Column(name = "estado_actual")
  private String estadoActualTexto;

//  @Enumerated(EnumType.STRING)
  // esto lo dejamos en Trasient porque implementamos el patron state para estados, la instrancia de cada objeto no se puede guardar en bbdd,por eso guardamos el texto y usamos postload para instanciar el estado dinamicamente
  @Transient
  private EstadoDonacionState estadoActual;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "donacion_id")
  @OrderBy("fechaYHora ASC")
  private List<RegistroCambioEstado> historialEstados = new ArrayList<>();

  //revisar esto TODO
  //(cascade = CascadeType.PERSIST)
  @ManyToOne
  @JoinColumn(name = "donante_id")
  private Donante donante;

  protected Donacion() {
  }

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
//    this.id = id;
    this.descripcionGeneral = descripcionGeneral;
    this.cantidad = cantidad;
    this.unidadMedida = unidadMedida;
    this.subcategoria = subcategoria;
    this.fechaDeRegistro = fechaDeRegistro;
    this.fechaVencimiento = fechaVencimiento;
    this.estadoBien = estadoBien;
    this.estadoActual = new EstadoDonacionEnDeposito();
    this.estadoActualTexto = this.estadoActual.getNombre().name();
    this.historialEstados = new ArrayList<>();
    this.donante = donante;
  }


  public void cambiarEstado(EstadoDonacion nuevoEstado, String justificativo) {
    this.estadoActual.cambiarA(this, nuevoEstado, justificativo);
  }

  void aplicarCambioEstado(
      EstadoDonacionState nuevoEstado,
      String justificativo
  ) {
    RegistroCambioEstado registro = new RegistroCambioEstado(
        this.estadoActual.getNombre(),
        nuevoEstado.getNombre(),
        justificativo
    );

    this.historialEstados.add(registro);
    this.estadoActual = nuevoEstado;
    this.estadoActualTexto = nuevoEstado.getNombre().name();
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


  public String getIdAsString() {
    return id != null ? id.toString() : null;
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

  public AsignacionDonacion asignarA(
      org.example.dominio.beneficiario.EntidadBeneficiaria entidad,
      org.example.dominio.notificacion.Notificador notificador) {
    java.util.Objects.requireNonNull(notificador, "El notificador es obligatorio");
    AsignacionDonacion asignacion = asignarA(entidad);
    notificador.notificarDonacionAsignadaBeneficiario(asignacion);
    notificador.notificarDonacionAsignadaDonante(asignacion);
    return asignacion;
  }

  @PostLoad
  private void reconstruirEstadoActual() {
    if (estadoActualTexto == null || estadoActualTexto.isBlank()) {
      throw new IllegalStateException(
          "La donacion persistida no tiene estado: " + id
      );
    }

    EstadoDonacion estado = EstadoDonacion.valueOf(estadoActualTexto);
    this.estadoActual = EstadoDonacionFactory.crear(estado);
  }

}

