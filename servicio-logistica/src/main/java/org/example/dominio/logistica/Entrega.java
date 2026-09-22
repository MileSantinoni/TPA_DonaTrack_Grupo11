package org.example.dominio.logistica;

import java.util.ArrayList;
import java.util.List;

// Logistica conserva una referencia y los datos de destino recibidos por HTTP.
// Donacion y EntidadBeneficiaria pertenecen al otro microservicio.
public class Entrega {
  private final String idDonacion;
  private final String idEntidad;
  private final String razonSocial;
  private final String direccion;
  private final String telefono;
  private final int orden;
  private EstadoEntrega estado = EstadoEntrega.PENDIENTE;
  private Camion camionResponsable;
  private java.time.LocalDateTime fechaRecepcion;
  private EventoLogistico eventoPendiente;
  private final List<String> fotosRecepcion = new ArrayList<>();

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

  private EventoLogistico publicar(EventoLogistico.Tipo tipo, String motivo,
                                    String patente, Donaciones donaciones)
      throws java.io.IOException, InterruptedException {
    java.util.Objects.requireNonNull(donaciones, "La conexion con Donaciones es obligatoria");
    if (motivo == null || motivo.isBlank()) throw new IllegalArgumentException("El motivo es obligatorio");
    if (eventoPendiente == null) {
      eventoPendiente = new EventoLogistico(java.util.UUID.randomUUID().toString(), tipo,
          List.of(new EventoLogistico.Referencia(idDonacion, idEntidad)), patente,
          motivo, java.time.LocalDateTime.now());
    } else if (eventoPendiente.tipo() != tipo || !eventoPendiente.motivo().equals(motivo)
        || !eventoPendiente.patente().equals(patente)) {
      throw new IllegalStateException("Reintente primero la operacion pendiente");
    }
    try {
      donaciones.informar(eventoPendiente);
    } catch (IllegalArgumentException | IllegalStateException rechazo) {
      eventoPendiente = null; // Rechazo HTTP definitivo: no se aplico el evento.
      throw rechazo;
    }
    EventoLogistico confirmado = eventoPendiente;
    eventoPendiente = null;
    return confirmado;
  }

  public void agregarFotoRecepcion(String foto) {
    exigirEstado(EstadoEntrega.ENTREGADA);
    if (foto == null || foto.isBlank()) throw new IllegalArgumentException("La foto es obligatoria");
    fotosRecepcion.add(foto);
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
  public List<String> getFotosRecepcion() { return List.copyOf(fotosRecepcion); }
}
