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

    var solicitud = new ClienteHeiGit.Solicitud(
        List.of(
            trabajo(1, -58.4106, -34.5889),
            trabajo(2, -58.4333, -34.6037),
            trabajo(3, -58.3732, -34.6212),
            trabajo(4, -58.4270, -34.6190)
        ),
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
        if ("job".equals(paso.path("type").asText())) {
          System.out.println(
              orden++ + ". " + paso.path("description").asText()
                  + " (id: " + paso.path("id").asInt() + ")"
          );
        }
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