package org.example.dominio.logistica;

import java.util.ArrayList;
import java.util.List;
import org.example.Repositorios.RepositorioCamiones;
import org.example.Repositorios.RepositorioRutas;

// Recibe los reportes de ubicación de la app móvil, los valida y los procesa
// para reflejar la posición y el avance de cada camión en el dashboard.
public class MonitorCamiones {

  private final RepositorioRutas rutas;

  public MonitorCamiones(RepositorioRutas rutas) {
    this.rutas = java.util.Objects.requireNonNull(rutas);
  }

  // Se registra una ruta para poder monitorearla (el camión ya salió del depósito)
  public void registrarRuta(Ruta ruta) {
    rutas.agregar(ruta);
  }

  public synchronized boolean iniciarRuta(
      String patente,
      Donaciones donaciones,
      RepositorioCamiones camiones
  ) throws java.io.IOException, InterruptedException {

    java.util.Objects.requireNonNull(
        donaciones, "La conexion con Donaciones es obligatoria"
    );

    Ruta ruta = buscarRutaPorPatente(patente);

    if (ruta == null) {
      return false;
    }

    // Preparar y confirmar en la BD el evento antes de enviarlo.
    rutas.enTransaccion(() -> {
      ruta.prepararInicio();
      rutas.actualizar(ruta);
    });

    // Devuelve el evento ya preparado, o null si la ruta estaba activa.
    EventoLogistico evento = ruta.prepararInicio();

    if (evento == null) {
      return true;
    }

    // La llamada HTTP se realiza fuera de la transacción de base de datos.
    try {
      donaciones.informar(evento);
    } catch (IllegalArgumentException | IllegalStateException rechazo) {
      rutas.enTransaccion(() -> {
        ruta.rechazarInicio(evento.idOperacion());
        rutas.actualizar(ruta);
      });

      throw rechazo;
    }

    // Si hubo IOException o InterruptedException, no llegamos acá:
    // el evento queda guardado para reintentar con el mismo ID.

    rutas.enTransaccion(() -> {
      ruta.confirmarInicio(evento.idOperacion());
      rutas.actualizar(ruta);
      camiones.actualizar(ruta.getCamion());
    });

    return true;
  }

  // Punto de entrada: recibe un reporte, lo valida y, si es válido, actualiza
  // la última ubicación del camión. Devuelve true si fue procesado.
  public synchronized boolean recibirReporte(
      ReporteUbicacion reporte,
      RepositorioCamiones camiones
  ) {
    Ruta ruta = buscarRutaPorPatente(reporte.getPatente());

    if (ruta == null || !ruta.estaActiva()) {
      return false;
    }

    if (!coordenadasValidas(reporte)
        || !Double.isFinite(reporte.getVelocidad())
        || reporte.getVelocidad() < 0
        || reporte.getFechaYHora() == null) {
      return false;
    }

    // Consultar el estado persistido, no la copia guardada en la ruta.
    Camion camion = camiones
        .buscarPorPatente(reporte.getPatente())
        .orElse(null);

    if (camion == null || esReporteAtrasado(camion, reporte)) {
      return false;
    }

    UbicacionCamion ubicacion = new UbicacionCamion(
        reporte.getLatitud(),
        reporte.getLongitud(),
        reporte.getVelocidad(),
        reporte.getFechaYHora()
    );

    camion.actualizarUbicacion(ubicacion);
    camiones.actualizar(camion);

    return true;
  }


  // Consulta para el dashboard: última posición conocida de un camión
  public UbicacionCamion ubicacionActual(String patente) {
    Ruta ruta = buscarRutaPorPatente(patente);
    if (ruta == null) {
      return null;
    }
    return ruta.getCamion().getUltimaUbicacion();
  }

  // Consulta para el dashboard: avance del camión sobre su ruta (0 a 100)
  public double avanceDeRuta(String patente) {
    Ruta ruta = buscarRutaPorPatente(patente);
    if (ruta == null) {
      return 0.0;
    }
    return ruta.porcentajeAvance();
  }

  // Devuelve la ruta en seguimiento de una patente (o null si no existe)
  public Ruta rutaDe(String patente) {
    return buscarRutaPorPatente(patente);
  }

  private Ruta buscarRutaPorPatente(String patente) {
    var encontradas = rutas.buscarPorPatente(patente);

    if (encontradas.size() > 1) {
      throw new IllegalStateException(
          "Hay varias rutas para el camion; falta identificar la ruta en seguimiento"
      );
    }

    return encontradas.isEmpty() ? null : encontradas.get(0);
  }

  private boolean coordenadasValidas(ReporteUbicacion reporte) {
      return Double.isFinite(reporte.getLatitud())
          && Double.isFinite(reporte.getLongitud())
          && reporte.getLatitud() >= -90
          && reporte.getLatitud() <= 90
          && reporte.getLongitud() >= -180
          && reporte.getLongitud() <= 180;
  }

  private boolean esReporteAtrasado(Camion camion, ReporteUbicacion reporte) {
    UbicacionCamion ultima = camion.getUltimaUbicacion();
    if (ultima == null) {
      return false;
    }
    return reporte.getFechaYHora().isBefore(ultima.getFechaYHora());
  }

  public synchronized boolean registrarRuta(PlanDeRuta plan,
      org.example.Repositorios.RepositorioCamiones camiones, Donaciones donaciones)
      throws java.io.IOException, InterruptedException {
    Camion camion = camiones.buscarPorPatente(plan.patente()).orElse(null);
    if (camion == null || !camion.estaDisponible()) return false;
    if (rutaDe(plan.patente()) != null) throw new IllegalStateException("El camion ya tiene una ruta registrada");
    if (plan.destinos().isEmpty()) throw new IllegalArgumentException("La ruta no tiene entregas");
    var asignaciones = donaciones.listarDestinos();
    Ruta ruta = new Ruta(camion, plan.deposito());
    var ids = new java.util.HashSet<String>();
    for (PlanDeRuta.Destino destino : plan.destinos()) {
      if (!ids.add(destino.idDonacion())) throw new IllegalStateException("La ruta contiene una donacion repetida");
      if (rutas.buscarTodos().stream().flatMap(r -> r.getEntregas().stream())
          .anyMatch(e -> e.getIdDonacion().equals(destino.idDonacion()) && !e.fueResuelta())) {
        throw new IllegalStateException("La donacion ya tiene una entrega pendiente");
      }
      var candidatas = asignaciones.stream().filter(a -> a.idDonacion().equals(destino.idDonacion())).toList();
      if (candidatas.isEmpty()) return false;
      if (candidatas.size() != 1) throw new IllegalStateException("La donacion tiene mas de una asignacion");
      var asignacion = candidatas.get(0);
      if (asignacion.idEntidad() == null || asignacion.idEntidad().isBlank()) {
        throw new IllegalStateException("La asignacion no tiene entidad destinataria");
      }
      // Los datos de destino autoritativos provienen de Donaciones, no del request.
      ruta.agregarEntrega(new Entrega(asignacion.idDonacion(), asignacion.idEntidad(),
          asignacion.razonSocial(), asignacion.direccion(), asignacion.telefono(), destino.orden()));
    }
    camion.marcarNoDisponible();

    rutas.enTransaccion(() -> {
      camion.marcarNoDisponible();
      rutas.agregar(ruta);
      camiones.actualizar(camion);
    });

    return true;
  }

}
