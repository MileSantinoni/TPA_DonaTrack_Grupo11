package org.example.service;

import org.example.Repositorios.RepositorioCamiones;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.api.logistica.dto.RutaRequest;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.Entrega;
import org.example.dominio.logistica.MonitorCamiones;
import org.example.dominio.logistica.Ruta;

public class RutaService {

  private final MonitorCamiones monitorCamiones;
  private final RepositorioCamiones repositorioCamiones;
  private final RepositorioDonaciones repositorioDonaciones;

  public RutaService() {
    this(MonitorCamiones.getInstance());
  }

  public RutaService(MonitorCamiones monitorCamiones) {
    this.monitorCamiones = monitorCamiones;
    this.repositorioCamiones = RepositorioCamiones.getInstance();
    this.repositorioDonaciones = RepositorioDonaciones.getInstance();
  }

  public boolean registrarRuta(RutaRequest request) {
    Camion camion = repositorioCamiones
        .buscarPorPatente(request.getPatente())
        .orElse(null);

    if (camion == null) {
      return false;
    }

    if (!camion.estaDisponible()) {
      return false;
    }

    Ruta ruta = new Ruta(camion);

    for (RutaRequest.EntregaRequest entregaRequest : request.getEntregas()) {
      Donacion donacion = repositorioDonaciones
          .buscarPorId(entregaRequest.getIdDonacion())
          .orElse(null);

      if (donacion == null) {
        return false;
      }

      EntidadBeneficiaria destino = new EntidadBeneficiaria(
          entregaRequest.getRazonSocial(),
          entregaRequest.getDireccion(),
          entregaRequest.getTelefono()
      );

      Entrega entrega = new Entrega(donacion, destino, entregaRequest.getOrden());
      ruta.agregarEntrega(entrega);
    }

    monitorCamiones.registrarRuta(ruta);
    camion.marcarNoDisponible();
    return true;
  }

}
