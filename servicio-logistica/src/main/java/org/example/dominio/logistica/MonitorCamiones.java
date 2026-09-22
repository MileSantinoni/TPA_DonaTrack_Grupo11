package org.example.dominio.logistica;

import java.util.ArrayList;
import java.util.List;

// Recibe los reportes de ubicación de la app móvil, los valida y los procesa
// para reflejar la posición y el avance de cada camión en el dashboard.
public class MonitorCamiones {

  private static MonitorCamiones instancia;
  private List<Ruta> rutasEnSeguimiento;

  public MonitorCamiones() {
    this.rutasEnSeguimiento = new ArrayList<>();
  }

  public static MonitorCamiones getInstance() {
    if (instancia == null) {
      instancia = new MonitorCamiones();
    }
    return instancia;
  }

  public void limpiar() {
    this.rutasEnSeguimiento.clear();
  }

  // Se registra una ruta para poder monitorearla (el camión ya salió del depósito)
  public void registrarRuta(Ruta ruta) {
    this.rutasEnSeguimiento.add(ruta);
  }

  public boolean iniciarRuta(String patente, Donaciones donaciones)
      throws java.io.IOException, InterruptedException {
    Ruta ruta = buscarRutaPorPatente(patente);
    if (ruta == null) {
      return false;
    }
    ruta.iniciar(donaciones);
    return true;
  }

  // Punto de entrada: recibe un reporte, lo valida y, si es válido, actualiza
  // la última ubicación del camión. Devuelve true si fue procesado.
  public boolean recibirReporte(ReporteUbicacion reporte) {
    Ruta ruta = buscarRutaPorPatente(reporte.getPatente());

    if (ruta == null) {
      return false; // no hay ruta en seguimiento para esa patente
    }
    if (!ruta.estaActiva()) {
      return false; // solo se acepta ubicación mientras la ruta está activa
    }
    if (!coordenadasValidas(reporte)) {
      return false;
    }
    if (reporte.getVelocidad() < 0) {
      return false;
    }
    if (esReporteAtrasado(ruta.getCamion(), reporte)) {
      return false; // llegó un reporte anterior al último registrado
    }

    UbicacionCamion ubicacion = new UbicacionCamion(
        reporte.getLatitud(),
        reporte.getLongitud(),
        reporte.getVelocidad(),
        reporte.getFechaYHora());
    ruta.getCamion().actualizarUbicacion(ubicacion);
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
    for (Ruta ruta : rutasEnSeguimiento) {
      if (ruta.getCamion().getPatente().equals(patente)) {
        return ruta;
      }
    }
    return null;
  }

  private boolean coordenadasValidas(ReporteUbicacion reporte) {
    if (reporte.getLatitud() < -90 || reporte.getLatitud() > 90) {
      return false;
    }
    if (reporte.getLongitud() < -180 || reporte.getLongitud() > 180) {
      return false;
    }
    return true;
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
      if (rutasEnSeguimiento.stream().flatMap(r -> r.getEntregas().stream())
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
    registrarRuta(ruta);
    camion.marcarNoDisponible();
    return true;
  }

}
