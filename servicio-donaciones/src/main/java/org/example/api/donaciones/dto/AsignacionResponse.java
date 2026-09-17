package org.example.api.donaciones.dto;

public class AsignacionResponse {

    private final String idAsignacion;
    private final String idDonacion;
    private final String idEntidad;
    private final String razonSocialEntidad;
    private final String estadoDonacion;

    public AsignacionResponse(String idAsignacion,
                              String idDonacion,
                              String idEntidad,
                              String razonSocialEntidad,
                              String estadoDonacion) {
        this.idAsignacion = idAsignacion;
        this.idDonacion = idDonacion;
        this.idEntidad = idEntidad;
        this.razonSocialEntidad = razonSocialEntidad;
        this.estadoDonacion = estadoDonacion;
    }

    public String getIdAsignacion() {
        return idAsignacion;
    }

    public String getIdDonacion() {
        return idDonacion;
    }

    public String getIdEntidad() {
        return idEntidad;
    }

    public String getRazonSocialEntidad() {
        return razonSocialEntidad;
    }

    public String getEstadoDonacion() {
        return estadoDonacion;
    }
}