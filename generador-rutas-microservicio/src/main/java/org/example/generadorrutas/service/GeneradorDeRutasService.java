package org.example.generadorrutas.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.example.generadorrutas.dto.AsignacionRutaRequest;
import org.example.generadorrutas.dto.CamionRutaRequest;
import org.example.generadorrutas.dto.EntregaGeneradaResponse;
import org.example.generadorrutas.dto.GenerarRutasRequest;
import org.example.generadorrutas.dto.RutaGeneradaResponse;
import org.example.generadorrutas.dto.UbicacionRequest;
import org.springframework.stereotype.Service;

@Service
public class GeneradorDeRutasService {

  public List<RutaGeneradaResponse> generar(GenerarRutasRequest request) {
    if (request.getAsignaciones().isEmpty() || request.getCamiones().isEmpty()) {
      return new ArrayList<>();
    }

    List<RutaGeneradaResponse> rutas = asignarRutas(request.getCamiones(), request.getAsignaciones());
    agregarUbicacionDeposito(rutas, request.getUbicacionDeposito());

    return rutas;
  }

  private List<RutaGeneradaResponse> asignarRutas(
      List<CamionRutaRequest> camionesDisponibles,
      List<AsignacionRutaRequest> asignacionesPlanificables
  ) {
    List<RutaGeneradaResponse> rutas = new ArrayList<>();
    List<List<AsignacionRutaRequest>> asignacionesPorEntidad =
        agruparAsignacionesPorEntidad(asignacionesPlanificables);

    for (int i = 0; i < asignacionesPorEntidad.size(); i++) {
      CamionRutaRequest camion = camionesDisponibles.get(i % camionesDisponibles.size());
      List<AsignacionRutaRequest> asignacionesDeEntidad = asignacionesPorEntidad.get(i);
      RutaGeneradaResponse ruta = new RutaGeneradaResponse();

      ruta.setPatenteCamion(camion.getPatente());
      agregarEntregas(ruta, asignacionesDeEntidad);

      rutas.add(ruta);
    }

    return rutas;
  }

  private List<List<AsignacionRutaRequest>> agruparAsignacionesPorEntidad(
      List<AsignacionRutaRequest> asignaciones
  ) {
    Map<String, List<AsignacionRutaRequest>> asignacionesPorEntidad = new LinkedHashMap<>();

    for (AsignacionRutaRequest asignacion : asignaciones) {
      String razonSocial = asignacion.getRazonSocialEntidad();

      if (!asignacionesPorEntidad.containsKey(razonSocial)) {
        asignacionesPorEntidad.put(razonSocial, new ArrayList<>());
      }

      asignacionesPorEntidad.get(razonSocial).add(asignacion);
    }

    return new ArrayList<>(asignacionesPorEntidad.values());
  }

  private void agregarEntregas(
      RutaGeneradaResponse ruta,
      List<AsignacionRutaRequest> asignaciones
  ) {
    for (AsignacionRutaRequest asignacion : asignaciones) {
      ruta.getEntregas().add(new EntregaGeneradaResponse(
          asignacion.getIdDonacion(),
          asignacion.getRazonSocialEntidad(),
          asignacion.getDireccionEntidad(),
          asignacion.getTelefonoEntidad(),
          ruta.getEntregas().size() + 1
      ));
    }
  }

  private void agregarUbicacionDeposito(
      List<RutaGeneradaResponse> rutas,
      UbicacionRequest ubicacionDeposito
  ) {
    for (RutaGeneradaResponse ruta : rutas) {
      ruta.setUbicacionDeposito(ubicacionDeposito);
    }
  }
}
