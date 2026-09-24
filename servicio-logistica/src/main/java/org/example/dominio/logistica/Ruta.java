package org.example.dominio.logistica;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import java.util.UUID;
import org.example.persistencia.EventoLogisticoConverter;

@Entity
@Table(name = "rutas")
public class Ruta {

  @Id
  private String id = UUID.randomUUID().toString();

  @ManyToOne(optional = false)
  @JoinColumn(name = "patente_camion", nullable = false)
  private Camion camion;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "ruta_id", nullable = false)
  @OrderBy("orden ASC")
  private List<Entrega> entregas = new ArrayList<>();

  private boolean activa;

  // Provisorio, igual que eventoPendiente en Entrega.
  @Convert(converter = EventoLogisticoConverter.class)
  @Column(name = "inicio_pendiente", length = 100000)
  private EventoLogistico inicioPendiente;

  @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
  @JoinColumn(name = "ubicacion_deposito_id")
  private UbicacionDeposito ubicacionDeposito;

  // Hibernate lo utiliza para reconstruir la entidad.
  protected Ruta() {
  }

  // Para crear rutas sin ubicación de depósito.
  public Ruta(Camion camion) {
    this(camion, null);
  }

  // Para crear rutas con ubicación de depósito.
  public Ruta(Camion camion, UbicacionCamion ubicacionDeposito) {
    this.camion = java.util.Objects.requireNonNull(
        camion, "El camion es obligatorio"
    );
    this.entregas = new ArrayList<>();
    this.activa = false;
    this.ubicacionDeposito = ubicacionDeposito == null
        ? null
        : new UbicacionDeposito(
        ubicacionDeposito.getLatitud(),
        ubicacionDeposito.getLongitud()
    );
  }

  public void agregarEntrega(Entrega entrega) {
    if (activa || inicioPendiente != null) throw new IllegalStateException("La ruta ya fue iniciada o tiene un inicio pendiente");
    this.entregas.add(entrega);
  }

  // El chofer da inicio a la ruta: pasa a activa y sus entregas quedan En Traslado
  public synchronized EventoLogistico prepararInicio() {
    if (activa) {
      return null;
    }

    if (inicioPendiente != null) {
      return inicioPendiente;
    }

    if (entregas.isEmpty()) {
      throw new IllegalStateException("La ruta no tiene entregas");
    }

    var ids = new java.util.HashSet<String>();

    for (Entrega entrega : entregas) {
      entrega.validarInicioTraslado();

      if (!ids.add(entrega.getIdDonacion())) {
        throw new IllegalStateException(
            "La ruta contiene una donacion repetida"
        );
      }
    }

    inicioPendiente = new EventoLogistico(
        java.util.UUID.randomUUID().toString(),
        EventoLogistico.Tipo.INICIO_TRASLADO,
        entregas.stream()
            .map(e -> new EventoLogistico.Referencia(
                e.getIdDonacion(), e.getIdEntidad()
            ))
            .toList(),
        camion.getPatente(),
        "El chofer inicio la ruta",
        java.time.LocalDateTime.now()
    );

    return inicioPendiente;
  }

  public synchronized void confirmarInicio(String idOperacion) {
    if (inicioPendiente == null
        || !inicioPendiente.idOperacion().equals(idOperacion)) {
      throw new IllegalStateException(
          "La confirmacion no corresponde al inicio pendiente"
      );
    }

    // Usamos la fecha original para que un reintento no genere
    // una ubicación de depósito posterior a un reporte GPS.
    java.time.LocalDateTime fechaInicio = inicioPendiente.fecha();

    entregas.forEach(Entrega::iniciarTraslado);
    activa = true;

    if (ubicacionDeposito != null) {
      UbicacionCamion ultima = camion.getUltimaUbicacion();

      if (ultima == null
          || ultima.getFechaYHora().isBefore(fechaInicio)) {
        camion.establecerUbicacionInicial(new UbicacionCamion(
            ubicacionDeposito.getLatitud(),
            ubicacionDeposito.getLongitud(),
            0,
            fechaInicio
        ));
      }
    }

    inicioPendiente = null;
  }

  public synchronized void rechazarInicio(String idOperacion) {
    if (inicioPendiente == null
        || !inicioPendiente.idOperacion().equals(idOperacion)) {
      throw new IllegalStateException(
          "El rechazo no corresponde al inicio pendiente"
      );
    }

    inicioPendiente = null;
  }

  // Conserva el comportamiento de los llamados actuales.
  public synchronized void iniciar(Donaciones donaciones)
      throws java.io.IOException, InterruptedException {

    java.util.Objects.requireNonNull(
        donaciones, "La conexion con Donaciones es obligatoria"
    );

    EventoLogistico evento = prepararInicio();

    if (evento == null) {
      return;
    }

    try {
      donaciones.informar(evento);
    } catch (IllegalArgumentException | IllegalStateException rechazo) {
      rechazarInicio(evento.idOperacion());
      throw rechazo;
    }

    // IOException o InterruptedException dejan el evento pendiente.
    confirmarInicio(evento.idOperacion());
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

  public String getId() {
    return id;
  }

  public UbicacionDeposito getUbicacionDeposito() {
    return ubicacionDeposito;
  }
}
