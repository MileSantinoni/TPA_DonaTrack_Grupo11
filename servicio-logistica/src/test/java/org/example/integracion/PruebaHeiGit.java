package org.example.integracion;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public class PruebaHeiGit {

  public static void main(String[] args) throws Exception {
    ClienteHeiGit cliente = new ClienteHeiGit(
        System.getenv("HEIGIT_API_KEY")
    );

    // Coordenadas en orden: longitud, latitud.
    List<Double> deposito = List.of(-58.3816, -34.6037);

    var donaciones = new ClienteDonaciones(
        System.getenv().getOrDefault("DONACIONES_URL", "http://localhost:8083")
    );

    var plan = TrabajosPlanificacion.preparar(
        donaciones.listarAsignaciones(),
        java.util.Set.of()
    );

    System.out.println("Pendientes por falta de ubicacion:");
    for (var pendiente : plan.pendientesDeUbicacion()) {
      System.out.println(pendiente.idDonacion() + " - " + pendiente.direccion());
    }

    if (plan.trabajos().isEmpty()) {
      System.out.println("No hay entregas que se puedan planificar.");
      return;
    }

    var solicitud = new ClienteHeiGit.Solicitud(
        plan.trabajos(),
        List.of(
            new ClienteHeiGit.Vehiculo(
                1,
                "Camion ABC123",
                "driving-car",
                deposito,
                deposito,
                List.of(2)
            ),
            new ClienteHeiGit.Vehiculo(
                2,
                "Camion DEF456",
                "driving-car",
                deposito,
                deposito,
                List.of(2)
            )
        )
    );

    JsonNode resultado = cliente.optimizar(solicitud);

    for (JsonNode ruta : resultado.path("routes")) {
      System.out.println(
          "\nVehiculo " + ruta.path("vehicle").asInt()
              + " - " + ruta.path("description").asText()
      );

      int orden = 1;

      for (JsonNode paso : ruta.path("steps")) {
        // Solo los pasos "job" representan entregas.
        if (!"job".equals(paso.path("type").asText())) {
          continue;
        }

        JsonNode id = paso.path("id");
        if (!id.isIntegralNumber() || !id.canConvertToInt()) {
          throw new IllegalStateException("Paso de entrega sin ID valido: " + paso);
        }

        int idTrabajo = id.intValue();
        var asignacion = plan.asignacionesPorTrabajo().get(idTrabajo);

        if (asignacion == null) {
          throw new IllegalStateException(
              "HeiGIT devolvio el trabajo " + idTrabajo
                  + ", pero se enviaron estos IDs: "
                  + plan.asignacionesPorTrabajo().keySet()
          );
        }

        System.out.println(
            orden++ + ". Donacion " + asignacion.idDonacion()
                + " -> " + asignacion.razonSocial()
                + " (" + asignacion.direccion() + ")"
        );
      }
    }

    System.out.println(
        "\nSin asignar: " + resultado.path("unassigned").toPrettyString()
    );
  }

  private static ClienteHeiGit.Trabajo trabajo(
      int id,
      double longitud,
      double latitud
  ) {
    return new ClienteHeiGit.Trabajo(
        id,
        "Entrega D" + id,
        List.of(longitud, latitud),
        List.of(1),
        300
    );
  }
}