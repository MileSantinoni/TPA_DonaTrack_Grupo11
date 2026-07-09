package org.example.service;

import org.example.Repositorios.RepositorioCamiones;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.api.logistica.dto.RutaRequest;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.Entrega;
import org.example.dominio.logistica.Ruta;
import org.example.dominio.logistica.UbicacionCamion;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RutaService {

  private final MonitoreoService monitoreoService;
  private final RepositorioCamiones repositorioCamiones;
  private final RepositorioDonaciones repositorioDonaciones;

  public RutaService(MonitoreoService monitoreoService) {
    this.monitoreoService = monitoreoService;
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

    if (!ubicacionDepositoValida(request)) {
      return false;
    }

    Ruta ruta = new Ruta(camion, crearUbicacionDeposito(request));

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
    camion.marcarNoDisponible();
    return true;
  }

  private UbicacionCamion crearUbicacionDeposito(RutaRequest request) {
    if (!tieneUbicacionDeposito(request)) {
      return null;
    }

    return new UbicacionCamion(
        request.getLatitudDeposito(),
        request.getLongitudDeposito(),
        0.0,
        LocalDateTime.now()
    );
  }

  private boolean ubicacionDepositoValida(RutaRequest request) {
    if (!tieneUbicacionDeposito(request)) {
      return true;
    }

    if (request.getLatitudDeposito() == null || request.getLongitudDeposito() == null) {
      return false;
    }

    return request.getLatitudDeposito() >= -90
        && request.getLatitudDeposito() <= 90
        && request.getLongitudDeposito() >= -180
        && request.getLongitudDeposito() <= 180;
  }

  private boolean tieneUbicacionDeposito(RutaRequest request) {
    return request.getLatitudDeposito() != null || request.getLongitudDeposito() != null;
  }

  public boolean iniciarRuta(String patente) {
    return monitoreoService.iniciarRuta(patente);
  }
}
