package org.example.service;

import org.example.dominio.logistica.MonitorCamiones;
import org.example.dominio.logistica.ReporteUbicacion;
import org.example.dominio.logistica.Ruta;
import org.example.dominio.logistica.UbicacionCamion;
import org.springframework.stereotype.Service;

// Une la capa REST con el dominio para monitorear rutas y ubicaciones.
@Service
public class MonitoreoService {

  private final MonitorCamiones monitor = new MonitorCamiones();

  public void registrarRuta(Ruta ruta) {
    monitor.registrarRuta(ruta);
  }

  public boolean iniciarRuta(String patente) {
    Ruta ruta = monitor.rutaDe(patente);
    if (ruta == null) {
      return false;
    }
    ruta.iniciar();
    return true;
  }

  public boolean recibirReporte(ReporteUbicacion reporte) {
    return monitor.recibirReporte(reporte);
  }

  public UbicacionCamion ubicacionActual(String patente) {
    return monitor.ubicacionActual(patente);
  }

  public double avanceDeRuta(String patente) {
    return monitor.avanceDeRuta(patente);
  }
}
