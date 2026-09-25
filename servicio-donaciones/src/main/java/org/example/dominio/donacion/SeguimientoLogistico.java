package org.example.dominio.donacion;

import java.util.*;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.notificacion.Notificador;

/** Aplica los eventos remotos sobre las donaciones y sus contactos locales. */
public class SeguimientoLogistico {
  private final RepositorioAsignacionesDonacion asignaciones;
  private final Notificador notificador;
  private final Map<String, EventoLogistico> procesados = new HashMap<>();

  public SeguimientoLogistico(RepositorioAsignacionesDonacion asignaciones, Notificador notificador) {
    this.asignaciones = Objects.requireNonNull(asignaciones);
    this.notificador = Objects.requireNonNull(notificador);
  }

  public synchronized void registrar(EventoLogistico evento) {
    EventoLogistico anterior = procesados.get(evento.idOperacion());
    if (anterior != null) {
      if (!anterior.equals(evento)) throw new IllegalStateException("El ID de operacion ya se uso con otros datos");
      return;
    }
    EstadoDonacion esperado = switch (evento.tipo()) {
      case INICIO_TRASLADO -> EstadoDonacion.LISTA_PARA_ENTREGAR;
      case RECEPCION, NO_RECIBIDA -> EstadoDonacion.EN_TRASLADO;
      case RETORNO_DEPOSITO -> EstadoDonacion.ENTREGA_FALLIDA;
    };
    EstadoDonacion siguiente = switch (evento.tipo()) {
      case INICIO_TRASLADO -> EstadoDonacion.EN_TRASLADO;
      case RECEPCION -> EstadoDonacion.ENTREGADA;
      case NO_RECIBIDA -> EstadoDonacion.ENTREGA_FALLIDA;
      case RETORNO_DEPOSITO -> EstadoDonacion.EN_DEPOSITO;
    };
    List<AsignacionDonacion> seleccionadas = new ArrayList<>();
    Set<String> ids = new HashSet<>();
    // Validar todo el lote antes de aplicar la primera transicion.
    for (EventoLogistico.Referencia referencia : evento.entregas()) {
      if (!ids.add(referencia.idDonacion())) throw new IllegalArgumentException("Donacion repetida en el evento");
      var candidatas = asignaciones.buscarTodas().stream()
          .filter(a -> a.getDonacion().getIdAsString().equals(referencia.idDonacion())).toList();
      if (candidatas.size() != 1) throw new IllegalStateException("No existe una asignacion unica para la donacion");
      AsignacionDonacion asignacion = candidatas.get(0);
      if (!asignacion.getEntidad().getIdAsString().equals(referencia.idEntidad())) {
        throw new IllegalStateException("La entidad destinataria no coincide con la asignacion");
      }
      if (asignacion.getDonacion().getEstadoActual() != esperado) {
        throw new IllegalStateException("La donacion debe estar " + esperado);
      }
      seleccionadas.add(asignacion);
    }
    for (AsignacionDonacion asignacion : seleccionadas) {
      asignacion.getDonacion().cambiarEstado(siguiente, evento.motivo());
      asignaciones.actualizar(asignacion);
    }
    procesados.put(evento.idOperacion(), evento);
    for (AsignacionDonacion asignacion : seleccionadas) {
      notificador.notificarEventoLogistico(asignacion, evento);
    }
  }
}