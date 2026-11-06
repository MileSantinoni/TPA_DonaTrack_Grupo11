package org.example.dominio.beneficiario;
import java.time.LocalDate;
import org.example.dominio.catalogo.Subcategoria;
import javax.persistence.*;


@Entity
@Table(name = "necesidades_recurrentes")
public class NecesidadRecurrente extends Necesidad {
  private LocalDate fechaInicioPeriodo;

  @Enumerated(EnumType.STRING)
  private Periodicidad periodicidad;

  protected NecesidadRecurrente() {}

  public NecesidadRecurrente(String descripcion, int cantidadObjetivo, Subcategoria subcategoria,
                             LocalDate fechaInicioPeriodo, Periodicidad periodicidad) {
    super(descripcion, cantidadObjetivo, subcategoria);
    this.fechaInicioPeriodo = fechaInicioPeriodo;
    this.periodicidad = periodicidad;
  }

  public Periodicidad getPeriodicidad() { return periodicidad; }

  @Override
  public boolean estaSatisfecha() {
    // La lógica para la recurrente implica verificar si se cubrió el objetivo dentro del período en curso
    return this.cantidadCubierta >= this.cantidadObjetivo;
    // Nota: en futuras iteraciones, aquí se podría agregar lógica para reiniciar
    // la "cantidadCubierta" cuando cambie el periodo (ej. pasa de semana).
  }

  public LocalDate getFechaInicioPeriodo() {
    return fechaInicioPeriodo;
  }

  public void actualizarPeriodo(
      LocalDate inicio,
      Periodicidad periodicidad
  ) {
    this.fechaInicioPeriodo =
        java.util.Objects.requireNonNull(inicio);

    this.periodicidad =
        java.util.Objects.requireNonNull(periodicidad);
  }

}