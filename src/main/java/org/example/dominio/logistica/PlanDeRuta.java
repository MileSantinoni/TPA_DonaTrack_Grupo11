package org.example.dominio.logistica;

import java.util.List;

public record PlanDeRuta(String patente, List<Destino> destinos, UbicacionCamion deposito) {
  public PlanDeRuta {
    destinos = List.copyOf(destinos);
  }

  public record Destino(String idDonacion, String razonSocial, String direccion,
                        String telefono, int orden) {}
}
