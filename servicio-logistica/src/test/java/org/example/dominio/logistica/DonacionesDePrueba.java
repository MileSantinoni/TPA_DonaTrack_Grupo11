package org.example.dominio.logistica;
import java.util.*;
import java.io.IOException;

public class DonacionesDePrueba implements Donaciones {
  public final List<EventoLogistico> eventos = new ArrayList<>();
  public List<Asignacion> asignaciones = List.of(
      new Asignacion("D1", "E1", "Comedor", "Calle 1", "123", "LISTA_PARA_ENTREGAR"),
      new Asignacion("D2", "E2", "Hogar", "Calle 2", "456", "LISTA_PARA_ENTREGAR"));
  public boolean rechazar;
  public boolean perderRespuesta;
  public List<Asignacion> listarDestinos() { return asignaciones; }
  public void informar(EventoLogistico evento)
      throws java.io.IOException, InterruptedException {
    if (rechazar) throw new IllegalStateException("La donacion no esta lista");
    eventos.add(evento);
    if (perderRespuesta) throw new IOException("Respuesta perdida");
  }
}
