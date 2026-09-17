package org.example.dominio.logistica;

import java.util.ArrayList;
import java.util.List;
import org.example.dominio.notificacion.Notificador;

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

  public boolean iniciarRuta(String patente, Notificador notificador) {
    Ruta ruta = buscarRutaPorPatente(patente);
    if (ruta == null) {
      return false;
    }
    ruta.iniciar(notificador);
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
  public boolean registrarRuta(PlanDeRuta plan,
      org.example.Repositorios.RepositorioCamiones camiones,
      java.util.function.Function<String, java.util.Optional<org.example.dominio.donacion.Donacion>> donaciones) {
    Camion camion = camiones.buscarPorPatente(plan.patente()).orElse(null);
    if (camion == null || !camion.estaDisponible()) return false;
    Ruta ruta = new Ruta(camion, plan.deposito());
    for (PlanDeRuta.Destino destino : plan.destinos()) {
      var donacion = donaciones.apply(destino.idDonacion()).orElse(null);
      if (donacion == null) return false;
      ruta.agregarEntrega(new Entrega(donacion,
          new org.example.dominio.beneficiario.EntidadBeneficiaria(
              destino.razonSocial(), destino.direccion(), destino.telefono()), destino.orden()));
    }
    registrarRuta(ruta);
    camion.marcarNoDisponible();
    return true;
  }

}
