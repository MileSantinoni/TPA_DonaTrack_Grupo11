package org.example.service;

import org.example.Repositorios.RepositorioDonaciones;
import org.example.api.logistica.dto.RutaRequest;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.Entrega;
import org.example.dominio.logistica.Ruta;
import org.springframework.stereotype.Service;

@Service
public class RutaService {

  private final MonitoreoService monitoreoService;
  private final RepositorioDonaciones repositorioDonaciones;

  public RutaService(MonitoreoService monitoreoService) {
    this.monitoreoService = monitoreoService;
    this.repositorioDonaciones = RepositorioDonaciones.getInstance();
  }

  public boolean registrarRuta(RutaRequest request) {
    Camion camion = monitoreoService.buscarCamion(request.getPatente());

    if (camion == null) {
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

      Entrega entrega = new Entrega(
          donacion,
          destino,
          entregaRequest.getOrden()
      );

      ruta.agregarEntrega(entrega);
    }

    monitoreoService.registrarRuta(ruta);
    return true;
  }

  public boolean iniciarRuta(String patente) {
    return monitoreoService.iniciarRuta(patente);
  }
}