package org.example.dominio.beneficiario;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDate;
import java.util.*;
import javax.persistence.*;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.donacion.Donacion;

@Entity
@Table(name = "necesidades")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Necesidad {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  protected String descripcion;

  @Column(name = "cantidad_objetivo")
  protected int cantidadObjetivo;

  @Column(name = "unidad_medida")
  private String unidadMedida;

  @ElementCollection
  @CollectionTable(
      name = "aportes_necesidad",
      joinColumns = @JoinColumn(name = "necesidad_id"),
      uniqueConstraints = @UniqueConstraint(
          columnNames = {"necesidad_id", "referencia"}
      )
  )
  @MapKeyColumn(name = "referencia")
  @JsonIgnore
  private Map<String, AporteNecesidad> aportes = new HashMap<>();

  @ManyToOne(cascade = CascadeType.MERGE)
  @JoinColumn(name = "subcategoria_id")
  protected Subcategoria subcategoria;

  @ManyToOne
  @JoinColumn(name = "entidad_beneficiaria_id")
  protected EntidadBeneficiaria entidadBeneficiaria;

  protected Necesidad() {
  }

  public Necesidad(
      String descripcion,
      int cantidadObjetivo,
      Subcategoria subcategoria
  ) {
    this.descripcion = descripcion;
    this.cantidadObjetivo = cantidadObjetivo;
    this.subcategoria = subcategoria;
  }

  void asociarA(EntidadBeneficiaria entidad) {
    Objects.requireNonNull(entidad);

    if (entidadBeneficiaria != null
        && entidadBeneficiaria != entidad) {
      throw new IllegalStateException(
          "La necesidad ya pertenece a otra entidad"
      );
    }

    entidadBeneficiaria = entidad;
  }

  public static String normalizarUnidad(String unidad) {
    return switch (
        Objects.toString(unidad, "")
            .trim()
            .toLowerCase(Locale.ROOT)
        ) {
      case "kg", "kilogramo", "kilogramos" -> "kg";
      case "l", "litro", "litros" -> "litros";
      case "unidad", "unidades" -> "unidades";
      case "caja", "cajas" -> "cajas";
      case "paquete", "paquetes" -> "paquetes";

      default -> throw new IllegalArgumentException(
          "Seleccioná una unidad de medida válida."
      );
    };
  }

  public void actualizarUnidad(String unidad) {
    String nueva = normalizarUnidad(unidad);

    if (!aportes.isEmpty()
        && !Objects.equals(nueva, unidadMedida)) {
      throw new IllegalStateException(
          "No se puede cambiar la unidad de una necesidad con aportes."
      );
    }

    unidadMedida = nueva;
  }

  public boolean compatibleCon(Donacion donacion) {
    if (unidadMedida == null
        || subcategoria == null
        || donacion.getSubcategoria() == null) {
      return false;
    }

    try {
      return Objects.equals(
          subcategoria.getId(),
          donacion.getSubcategoria().getId()
      ) && unidadMedida.equals(
          normalizarUnidad(donacion.getUnidadMedida())
      );
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  public boolean puedeRecibir(
      Donacion donacion,
      LocalDate fecha
  ) {
    return compatibleCon(donacion)
        && activaEn(fecha)
        && cantidadCubiertaEn(fecha) < cantidadObjetivo;
  }

  protected boolean activaEn(LocalDate fecha) {
    return true;
  }

  protected boolean perteneceAlPeriodo(
      LocalDate aporte,
      LocalDate consulta
  ) {
    return !aporte.isAfter(consulta);
  }

  public void registrarRecepcion(
      String referencia,
      int cantidad,
      LocalDate fecha
  ) {
    if (referencia == null
        || referencia.isBlank()
        || cantidad <= 0
        || fecha == null) {
      throw new IllegalArgumentException(
          "El aporte requiere referencia, cantidad positiva y fecha."
      );
    }

    AporteNecesidad previo = aportes.get(referencia);

    if (previo != null) {
      if (previo.getCantidad() != cantidad
          || !previo.getFecha().equals(fecha)) {
        throw new IllegalStateException(
            "La recepción ya se registró con otros datos."
        );
      }

      return;
    }

    if (!activaEn(fecha)) {
      throw new IllegalStateException(
          "La necesidad todavía no estaba vigente."
      );
    }

    aportes.put(
        referencia,
        new AporteNecesidad(cantidad, fecha)
    );
  }

  // Conserva los usos existentes del dominio.
  // Logística utiliza registrarRecepcion con el ID de asignación.
  public void registrarDonacion(int cantidad) {
    registrarRecepcion(
        UUID.randomUUID().toString(),
        cantidad,
        LocalDate.now()
    );
  }

  public int cantidadCubiertaEn(LocalDate fecha) {
    if (!activaEn(fecha)) {
      return 0;
    }

    int total = 0;

    for (AporteNecesidad aporte : aportes.values()) {
      if (perteneceAlPeriodo(aporte.getFecha(), fecha)) {
        total = Math.addExact(total, aporte.getCantidad());
      }
    }

    return total;
  }

  public boolean estaSatisfecha() {
    return getCantidadCubierta() >= cantidadObjetivo;
  }

  public boolean tieneAportes() {
    return !aportes.isEmpty();
  }

  public void actualizarDatos(
      String descripcion,
      int objetivo,
      Subcategoria subcategoria
  ) {
    if (descripcion == null
        || descripcion.isBlank()
        || subcategoria == null
        || objetivo <= 0) {
      throw new IllegalArgumentException(
          "La descripción, subcategoría y cantidad positiva son obligatorias."
      );
    }

    this.descripcion = descripcion.trim();
    this.cantidadObjetivo = objetivo;
    this.subcategoria = subcategoria;
  }

  public Long getId() {
    return id;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public int getCantidadObjetivo() {
    return cantidadObjetivo;
  }

  public int getCantidadCubierta() {
    return cantidadCubiertaEn(LocalDate.now());
  }

  public int getCantidadPendiente() {
    return Math.max(
        0,
        cantidadObjetivo - getCantidadCubierta()
    );
  }

  public String getUnidadMedida() {
    return unidadMedida;
  }

  public Subcategoria getSubcategoria() {
    return subcategoria;
  }

  public void setSubcategoria(Subcategoria subcategoria) {
    this.subcategoria = subcategoria;
  }

  public EntidadBeneficiaria getEntidadBeneficiaria() {
    return entidadBeneficiaria;
  }

  public void setEntidadBeneficiaria(
      EntidadBeneficiaria entidad
  ) {
    this.entidadBeneficiaria = entidad;
  }
}