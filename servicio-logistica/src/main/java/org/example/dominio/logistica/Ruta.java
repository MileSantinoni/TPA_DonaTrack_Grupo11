package org.example.dominio.logistica;

import java.util.ArrayList;
import java.util.List;

public class Ruta {

  private Camion camion;
  private List<Entrega> entregas;
  private boolean activa;
  private EventoLogistico inicioPendiente;
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
    if (activa || inicioPendiente != null) throw new IllegalStateException("La ruta ya fue iniciada o tiene un inicio pendiente");
    this.entregas.add(entrega);
  }

  // El chofer da inicio a la ruta: pasa a activa y sus entregas quedan En Traslado
  public synchronized void iniciar(Donaciones donaciones)
      throws java.io.IOException, InterruptedException {
    java.util.Objects.requireNonNull(donaciones, "La conexion con Donaciones es obligatoria");
    if (activa) return;
    if (entregas.isEmpty()) throw new IllegalStateException("La ruta no tiene entregas");
    var ids = new java.util.HashSet<String>();
    for (Entrega entrega : entregas) {
      entrega.validarInicioTraslado();
      if (!ids.add(entrega.getIdDonacion())) {
        throw new IllegalStateException("La ruta contiene una donacion repetida");
      }
    }
    if (inicioPendiente == null) {
      inicioPendiente = new EventoLogistico(java.util.UUID.randomUUID().toString(),
          EventoLogistico.Tipo.INICIO_TRASLADO,
          entregas.stream().map(e -> new EventoLogistico.Referencia(e.getIdDonacion(), e.getIdEntidad())).toList(),
          camion.getPatente(), "El chofer inicio la ruta", java.time.LocalDateTime.now());
    }
    // Donaciones valida todo el lote antes de cambiar estados. Un reintento usa el mismo ID.
    try {
      donaciones.informar(inicioPendiente);
    } catch (IllegalArgumentException | IllegalStateException rechazo) {
      inicioPendiente = null;
      throw rechazo;
    }
    entregas.forEach(Entrega::iniciarTraslado);
    activa = true;
    inicioPendiente = null;
    if (ubicacionDeposito != null) camion.establecerUbicacionInicial(ubicacionDeposito);
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
    return List.copyOf(entregas);
  }

  public boolean estaActiva() {
    return activa;
  }
}
