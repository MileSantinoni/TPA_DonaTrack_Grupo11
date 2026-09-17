package org.example.integracion;

// Copia del contrato HTTP; no comparte entidades Java con Donaciones.
public record AsignacionDisponible(
    String idDonacion,
    String idEntidad,
    String razonSocial,
    String direccion,
    String telefono,
    String estadoDonacion
) {}
