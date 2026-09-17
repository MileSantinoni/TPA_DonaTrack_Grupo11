package org.example.dominio.logistica;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import org.example.dominio.notificacion.Notificador;

public class Ruta {

  private Camion camion;
  private List<Entrega> entregas;
  private boolean activa;
  private UbicacionCamion ubicacionDeposito;

  public Ruta(Camion camion) {
    this(camion, null);
  }

  public Ruta(Camion camion, UbicacionCamion ubicacionDeposito) {
    this.camion = camion;
    this.entregas = new ArrayList<>();
    this.activa = false;
    this.ubicacionDeposito = ubicacionDeposito;
  }

  public void agregarEntrega(Entrega entrega) {
    this.entregas.add(entrega);
  }

  // El chofer da inicio a la ruta: pasa a activa y sus entregas quedan En Traslado
  public void iniciar(Notificador notificador) {
    Objects.requireNonNull(notificador, "El notificador es obligatorio");
    if (activa) {
      return;
    }
    var donaciones = new HashSet<String>();
    for (Entrega entrega : entregas) {
      entrega.validarInicioTraslado();
      if (!donaciones.add(entrega.getDonacion().getId())) {
        throw new IllegalStateException("La ruta contiene una donacion repetida");
      }
    }
    for (Entrega entrega : entregas) {
      entrega.iniciarTraslado();
    }
    this.activa = true;
    if (ubicacionDeposito != null) {
      camion.establecerUbicacionInicial(ubicacionDeposito);
    }
    notificador.notificarInicioRuta(this);
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
