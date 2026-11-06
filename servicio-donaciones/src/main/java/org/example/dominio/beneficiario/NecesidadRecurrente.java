package org.example.dominio.beneficiario;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import javax.persistence.*;
import org.example.dominio.catalogo.Subcategoria;

@Entity
@Table(name = "necesidades_recurrentes")
public class NecesidadRecurrente extends Necesidad {

  private LocalDate fechaInicioPeriodo;

  @Enumerated(EnumType.STRING)
  private Periodicidad periodicidad;

  protected NecesidadRecurrente() {
  }

  public NecesidadRecurrente(
      String descripcion,
      int cantidadObjetivo,
      Subcategoria subcategoria,
      LocalDate fechaInicioPeriodo,
      Periodicidad periodicidad
  ) {
    super(descripcion, cantidadObjetivo, subcategoria);

    this.fechaInicioPeriodo = fechaInicioPeriodo;
    this.periodicidad = periodicidad;
  }

  public LocalDate getFechaInicioPeriodo() {
    return fechaInicioPeriodo;
  }

  public Periodicidad getPeriodicidad() {
    return periodicidad;
  }

  public void actualizarPeriodo(
      LocalDate inicio,
      Periodicidad periodicidad
  ) {
    Objects.requireNonNull(inicio);
    Objects.requireNonNull(periodicidad);

    if (tieneAportes()
        && (!inicio.equals(fechaInicioPeriodo)
        || periodicidad != this.periodicidad)) {
      throw new IllegalStateException(
          "No se puede cambiar el período de una necesidad con aportes."
      );
    }

    this.fechaInicioPeriodo = inicio;
    this.periodicidad = periodicidad;
  }

  @Override
  protected boolean activaEn(LocalDate fecha) {
    return fechaInicioPeriodo != null
        && periodicidad != null
        && !fecha.isBefore(fechaInicioPeriodo);
  }

  public LocalDate inicioPeriodoEn(LocalDate fecha) {
    if (!activaEn(fecha)) {
      return fechaInicioPeriodo;
    }

    long numero = switch (periodicidad) {
      case SEMANAL ->
          ChronoUnit.DAYS.between(fechaInicioPeriodo, fecha) / 7;

      case MENSUAL ->
          ChronoUnit.MONTHS.between(
              YearMonth.from(fechaInicioPeriodo),
              YearMonth.from(fecha)
          );

      case ANUAL ->
          (long) fecha.getYear() - fechaInicioPeriodo.getYear();
    };

    LocalDate inicio = desplazar(numero);

    return inicio.isAfter(fecha)
        ? desplazar(numero - 1)
        : inicio;
  }

  private LocalDate desplazar(long numero) {
    return switch (periodicidad) {
      case SEMANAL -> fechaInicioPeriodo.plusWeeks(numero);
      case MENSUAL -> fechaInicioPeriodo.plusMonths(numero);
      case ANUAL -> fechaInicioPeriodo.plusYears(numero);
    };
  }

  @Override
  protected boolean perteneceAlPeriodo(
      LocalDate aporte,
      LocalDate consulta
  ) {
    return !aporte.isBefore(inicioPeriodoEn(consulta))
        && !aporte.isAfter(consulta);
  }
}