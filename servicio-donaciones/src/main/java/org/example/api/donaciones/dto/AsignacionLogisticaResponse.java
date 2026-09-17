package org.example.api.donaciones.dto;

public record AsignacionLogisticaResponse(
    String idDonacion,
    String idEntidad,
    String razonSocial,
    String direccion,
    String telefono,
    String estadoDonacion
) {}
