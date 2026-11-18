package org.example.dominio.logistica;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;
import java.util.UUID;
import org.example.persistencia.EventoLogisticoConverter;

@Entity
@Table(name = "entregas")
public class Entrega {

  @Id
  private String id = UUID.randomUUID().toString();

  @Column(name = "id_donacion", nullable = false)
  private String idDonacion;

  @Column(name = "id_entidad_destino", nullable = false)
  private String idEntidad;

  private String razonSocial;
  private String direccion;
  private String telefono;
  private int orden;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EstadoEntrega estado = EstadoEntrega.PENDIENTE;

  @ManyToOne
  @JoinColumn(name = "patente_camion_responsable")
  private Camion camionResponsable;

  private java.time.LocalDateTime fechaRecepcion;

  // Provisorio: todavía no persistimos los eventos pendientes.
  @Convert(converter = EventoLogisticoConverter.class)
  @Column(name = "evento_pendiente", length = 100000)
  private EventoLogistico eventoPendiente;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "entrega_id", nullable = false)
  @OrderColumn(name = "orden_foto")
  private List<FotoRecepcion> fotosRecepcion = new ArrayList<>();

  @Column(name = "geometria_hasta_entrega", columnDefinition = "text")
  private String geometriaHastaEntrega;

  @Column(name = "llegada_destino_utc")
  private java.time.Instant llegadaDestinoUtc;

  protected Entrega() {
    // Utilizado por JPA.
  }

  public Entrega(String idDonacion, String idEntidad, String razonSocial,
                 String direccion, String telefono, int orden) {
    this.idDonacion = idDonacion;
    this.idEntidad = idEntidad;
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
    this.orden = orden;
  }

  public void validarInicioTraslado() {
    exigirEstado(EstadoEntrega.PENDIENTE);
    if (eventoPendiente != null) throw new IllegalStateException("Hay una operacion pendiente");
  }

  // Solo Ruta lo invoca luego de la confirmacion HTTP del lote completo.
  void iniciarTraslado() { estado = EstadoEntrega.EN_TRASLADO; }

  public synchronized void confirmarRecepcion(Camion camion, Donaciones donaciones)
      throws java.io.IOException, InterruptedException {
    java.util.Objects.requireNonNull(camion, "El camion responsable es obligatorio");
    exigirEstado(EstadoEntrega.EN_TRASLADO);
    EventoLogistico evento = publicar(EventoLogistico.Tipo.RECEPCION,
        "La entidad confirmo la recepcion", camion.getPatente(), donaciones);
    estado = EstadoEntrega.ENTREGADA;
    camionResponsable = camion;
    fechaRecepcion = evento.fecha();
  }

  public synchronized void marcarNoRecibida(String motivo, Camion camion, Donaciones donaciones)
      throws java.io.IOException, InterruptedException {
    exigirEstado(EstadoEntrega.EN_TRASLADO);
    publicar(EventoLogistico.Tipo.NO_RECIBIDA, motivo, camion.getPatente(), donaciones);
    estado = EstadoEntrega.NO_RECIBIDA;
  }

  public synchronized void registrarRetornoADeposito(String motivo, Camion camion, Donaciones donaciones)
      throws java.io.IOException, InterruptedException {
    exigirEstado(EstadoEntrega.NO_RECIBIDA);
    publicar(EventoLogistico.Tipo.RETORNO_DEPOSITO, motivo, camion.getPatente(), donaciones);
    estado = EstadoEntrega.PENDIENTE;
  }

  private EventoLogistico publicar(
      EventoLogistico.Tipo tipo,
      String motivo,
      String patente,
      Donaciones donaciones
  ) throws java.io.IOException, InterruptedException {

    java.util.Objects.requireNonNull(
        donaciones, "La conexion con Donaciones es obligatoria"
    );

    EventoLogistico evento = prepararEvento(tipo, motivo, patente);

    try {
      donaciones.informar(evento);
    } catch (IllegalArgumentException | IllegalStateException rechazo) {
      eventoPendiente = null;
      throw rechazo;
    }

    // Ante IOException o InterruptedException, queda pendiente.
    eventoPendiente = null;
    return evento;
  }

  public void agregarFotoRecepcion(String foto) {
    exigirEstado(EstadoEntrega.ENTREGADA);
    if (foto == null || foto.isBlank()) throw new IllegalArgumentException("La foto es obligatoria");
    fotosRecepcion.add(new FotoRecepcion(foto));
  }

  private void exigirEstado(EstadoEntrega esperado) {
    if (estado != esperado) throw new IllegalStateException("La entrega debe estar " + esperado);
  }

  public java.time.LocalDateTime getFechaRecepcion() { return fechaRecepcion; }

  public boolean fueResuelta() {
    return estado == EstadoEntrega.ENTREGADA || estado == EstadoEntrega.NO_RECIBIDA;
  }

  public String getIdDonacion() { return idDonacion; }
  public String getIdEntidad() { return idEntidad; }
  public String getRazonSocial() { return razonSocial; }
  public String getDireccion() { return direccion; }
  public String getTelefono() { return telefono; }
  public int getOrden() { return orden; }
  public EstadoEntrega getEstado() { return estado; }
  public Camion getCamionResponsable() { return camionResponsable; }
  public List<String> getFotosRecepcion() {
    return fotosRecepcion.stream()
        .map(FotoRecepcion::getUrl)
        .toList();
  }

  public String getId() {
    return id;
  }

  public synchronized EventoLogistico prepararRecepcion(Camion camion) {
    exigirEstado(EstadoEntrega.EN_TRASLADO);

    return prepararEvento(
        EventoLogistico.Tipo.RECEPCION,
        "La entidad confirmo la recepcion",
        camion
    );
  }

  public synchronized EventoLogistico prepararNoRecibida(
      String motivo,
      Camion camion
  ) {
    exigirEstado(EstadoEntrega.EN_TRASLADO);

    return prepararEvento(
        EventoLogistico.Tipo.NO_RECIBIDA,
        motivo,
        camion
    );
  }

  public synchronized EventoLogistico prepararRetorno(
      String motivo,
      Camion camion
  ) {
    exigirEstado(EstadoEntrega.NO_RECIBIDA);

    return prepararEvento(
        EventoLogistico.Tipo.RETORNO_DEPOSITO,
        motivo,
        camion
    );
  }

  private EventoLogistico prepararEvento(
      EventoLogistico.Tipo tipo,
      String motivo,
      Camion camion
  ) {
    java.util.Objects.requireNonNull(
        camion, "El camion responsable es obligatorio"
    );

    return prepararEvento(tipo, motivo, camion.getPatente());
  }

  private EventoLogistico prepararEvento(
      EventoLogistico.Tipo tipo,
      String motivo,
      String patente
  ) {
    if (motivo == null || motivo.isBlank()) {
      throw new IllegalArgumentException("El motivo es obligatorio");
    }

    if (eventoPendiente == null) {
      eventoPendiente = new EventoLogistico(
          java.util.UUID.randomUUID().toString(),
          tipo,
          List.of(new EventoLogistico.Referencia(idDonacion, idEntidad)),
          patente,
          motivo,
          java.time.LocalDateTime.now()
      );
    } else if (eventoPendiente.tipo() != tipo
        || !eventoPendiente.motivo().equals(motivo)
        || !eventoPendiente.patente().equals(patente)) {
      throw new IllegalStateException(
          "Reintente primero la operacion pendiente"
      );
    }

    return eventoPendiente;
  }

  public String getGeometriaHastaEntrega() {
    return geometriaHastaEntrega;
  }

  public void guardarGeometriaHastaEntrega(String geometria) {
    if (geometria == null || geometria.isBlank()) {
      throw new IllegalArgumentException(
          "Falta el recorrido hasta la entrega."
      );
    }

    this.geometriaHastaEntrega = geometria;
  }

  public boolean llegoADestino() {
    return llegadaDestinoUtc != null;
  }

  public void registrarLlegadaSiCorresponde(
      double latitud,
      double longitud,
      java.time.Instant fechaReporte
  ) {
    if (llegoADestino()
        || estado != EstadoEntrega.EN_TRASLADO
        || geometriaHastaEntrega == null
        || geometriaHastaEntrega.isBlank()) {
      return;
    }

    double[] destino = obtenerUltimoPunto(geometriaHastaEntrega);

    double lat1 = Math.toRadians(latitud);
    double lat2 = Math.toRadians(destino[0]);
    double diferenciaLat = lat2 - lat1;
    double diferenciaLon = Math.toRadians(destino[1] - longitud);

    double a =
        Math.sin(diferenciaLat / 2) * Math.sin(diferenciaLat / 2)
            + Math.cos(lat1) * Math.cos(lat2)
            * Math.sin(diferenciaLon / 2) * Math.sin(diferenciaLon / 2);

    double distanciaMetros = 6371000 * 2
        * Math.atan2(
        Math.sqrt(Math.max(0, Math.min(1, a))),
        Math.sqrt(Math.max(0, 1 - Math.min(1, a)))
    );

    if (distanciaMetros <= 100) {
      llegadaDestinoUtc = fechaReporte;
    }
  }

  private static double[] obtenerUltimoPunto(String geometria) {
    int[] indice = {0};
    long latitud = 0;
    long longitud = 0;

    while (indice[0] < geometria.length()) {
      latitud += leerComponente(geometria, indice);
      longitud += leerComponente(geometria, indice);
    }

    double lat = latitud / 100000.0;
    double lon = longitud / 100000.0;

    if (Math.abs(lat) > 90 || Math.abs(lon) > 180) {
      throw new IllegalArgumentException(
          "El recorrido contiene coordenadas inválidas."
      );
    }

    return new double[]{lat, lon};
  }

  private static long leerComponente(String geometria, int[] indice) {
    long resultado = 0;
    int desplazamiento = 0;

    while (true) {
      if (indice[0] >= geometria.length() || desplazamiento > 30) {
        throw new IllegalArgumentException(
            "El recorrido guardado es inválido."
        );
      }

      int valor = geometria.charAt(indice[0]++) - 63;

      if (valor < 0 || valor > 63) {
        throw new IllegalArgumentException(
            "El recorrido guardado es inválido."
        );
      }

      resultado |= (long) (valor & 31) << desplazamiento;

      if (valor < 32) {
        return (resultado & 1) != 0
            ? ~(resultado >> 1)
            : resultado >> 1;
      }

      desplazamiento += 5;
    }
  }
}
