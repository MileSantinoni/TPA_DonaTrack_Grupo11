package org.example.api.donaciones.dto;

public class ConfirmarAsignacionRequest {

    private String idDonacion;
    private String idEntidad;

    public String getIdDonacion() {
        return idDonacion;
    }

    public void setIdDonacion(String idDonacion) {
        this.idDonacion = idDonacion;
    }

    public String getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(String idEntidad) {
        this.idEntidad = idEntidad;
    }
}