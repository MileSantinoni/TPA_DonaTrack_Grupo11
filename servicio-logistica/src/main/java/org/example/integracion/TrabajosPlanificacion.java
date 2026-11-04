package org.example.integracion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record TrabajosPlanificacion(
    List<ClienteHeiGit.Trabajo> trabajos,
    Map<Integer, AsignacionDisponible> asignacionesPorTrabajo,
    List<AsignacionDisponible> pendientesDeUbicacion
) {
  public TrabajosPlanificacion {
    trabajos = List.copyOf(trabajos);
    asignacionesPorTrabajo = Map.copyOf(asignacionesPorTrabajo);
    pendientesDeUbicacion = List.copyOf(pendientesDeUbicacion);
  }

  public static TrabajosPlanificacion preparar(
      List<AsignacionDisponible> asignaciones,
      Set<String> donacionesConRuta
  ) {
    List<ClienteHeiGit.Trabajo> trabajos = new ArrayList<>();
    Map<Integer, AsignacionDisponible> porTrabajo = new LinkedHashMap<>();
    List<AsignacionDisponible> pendientes = new ArrayList<>();
    Set<String> vistas = new HashSet<>();

    for (AsignacionDisponible asignacion : asignaciones) {
      if (!"ASIGNACION_REALIZADA".equals(asignacion.estadoDonacion())) {
        continue;
      }

      String idDonacion = asignacion.idDonacion();

      if (idDonacion == null || idDonacion.isBlank()) {
        throw new IllegalArgumentException("Asignacion sin ID de donacion");
      }

      if (donacionesConRuta.contains(idDonacion)) {
        continue;
      }

      if (!vistas.add(idDonacion)) {
        throw new IllegalArgumentException(
            "Hay dos asignaciones para la donacion " + idDonacion
        );
      }

      if (!asignacion.tieneUbicacion()) {
        pendientes.add(asignacion);
        continue;
      }

      int idTrabajo = trabajos.size() + 1;

      trabajos.add(new ClienteHeiGit.Trabajo(
          idTrabajo,
          "Donacion " + idDonacion,
          asignacion.coordenadasParaHeiGit(),
          List.of(1),
          0
      ));

      porTrabajo.put(idTrabajo, asignacion);
    }

    return new TrabajosPlanificacion(trabajos, porTrabajo, pendientes);
  }
}