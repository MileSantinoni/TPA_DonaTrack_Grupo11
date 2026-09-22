package org.example.dominio.logistica;

import java.io.IOException;
import java.util.List;

/** Contrato que necesita Logistica; la implementacion HTTP vive en integracion. */
public interface Donaciones {
  List<Asignacion> listarDestinos() throws IOException, InterruptedException;
  void informar(EventoLogistico evento) throws IOException, InterruptedException;

  record Asignacion(String idDonacion, String idEntidad, String razonSocial,
                    String direccion, String telefono, String estadoDonacion) {}
}
