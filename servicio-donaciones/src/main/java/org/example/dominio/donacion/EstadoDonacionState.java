package org.example.dominio.donacion;

public interface EstadoDonacionState {

  EstadoDonacion getNombre();

  void cambiarA(Donacion donacion, EstadoDonacion nuevoEstado, String justificativo);
}
