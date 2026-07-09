package org.example.service;

import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.MonitorCamiones;
import org.example.dominio.logistica.ReporteUbicacion;
import org.example.dominio.logistica.Ruta;
import org.example.dominio.logistica.UbicacionCamion;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

// Une la capa REST con el dominio: guarda la flota y el monitor en memoria.
@Service
public class MonitoreoService {

  private final MonitorCamiones monitor = new MonitorCamiones();
  private final List<Camion> flota = new ArrayList<>();

  public void registrarCamion(Camion camion) {
    flota.add(camion);
  }

  public Camion buscarCamion(String patente) {
    for (Camion camion : flota) {
      if (camion.getPatente().equals(patente)) {
        return camion;
      }
    }
    return null;
  }

  public List<Camion> getFlota() {
    return flota;
  }

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