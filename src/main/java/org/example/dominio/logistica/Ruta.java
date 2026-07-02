package org.example.dominio.logistica;

import java.util.ArrayList;
import java.util.List;

public class Ruta {

  private Camion camion;
  private List<Entrega> entregas;
  private boolean activa;

  public Ruta(Camion camion) {
    this.camion = camion;
    this.entregas = new ArrayList<>();
    this.activa = false;
  }

  public void agregarEntrega(Entrega entrega) {
    this.entregas.add(entrega);
  }

  // El chofer da inicio a la ruta: pasa a activa y sus entregas quedan En Traslado
  public void iniciar() {
    this.activa = true;
    for (Entrega entrega : entregas) {
      if (entrega.getEstado() == EstadoEntrega.PENDIENTE) {
        entrega.marcarEnTraslado();
      }
    }
  }

  public int cantidadEntregas() {
    return entregas.size();
  }

  public int cantidadEntregadas() {
    int contador = 0;
    for (Entrega entrega : entregas) {
      if (entrega.getEstado() == EstadoEntrega.ENTREGADA) {
        contador = contador + 1;
      }
    }
    return contador;
  }

  // Avance del recorrido para mostrar en el dashboard (0 a 100)
  public double porcentajeAvance() {
    if (entregas.isEmpty()) {
      return 0.0;
    }
    double total = cantidadEntregas();
    double entregadas = cantidadEntregadas();
    return (entregadas * 100.0) / total;
  }

  public Camion getCamion() {
    return camion;
  }

  public List<Entrega> getEntregas() {
    return entregas;
  }

  public boolean estaActiva() {
    return activa;
  }
}