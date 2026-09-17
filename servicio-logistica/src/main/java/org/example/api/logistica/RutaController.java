package org.example.api.logistica;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.io.IOException;
import org.example.Repositorios.RepositorioCamiones;
import org.example.api.logistica.dto.RutaRequest;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.Entrega;
import org.example.dominio.logistica.MonitorCamiones;
import org.example.dominio.logistica.Ruta;
import org.example.integracion.ClienteDonaciones;

public class RutaController {
  private final RepositorioCamiones camiones;
  private final MonitorCamiones monitor;
  private final ClienteDonaciones donaciones;

  public RutaController(RepositorioCamiones camiones, MonitorCamiones monitor,
                        ClienteDonaciones donaciones) {
    this.camiones = camiones;
    this.monitor = monitor;
    this.donaciones = donaciones;
  }

  public void registrarRuta(Context ctx) {
    RutaRequest request = ctx.bodyAsClass(RutaRequest.class);
    Camion camion = camiones.buscarPorPatente(request.getPatente()).orElse(null);
    if (camion == null || !camion.estaDisponible()) {
      ctx.status(HttpStatus.NOT_FOUND).result("No se pudo registrar la ruta");
      return;
    }

    Ruta ruta = new Ruta(camion);
    try {
      for (RutaRequest.EntregaRequest pedido : request.getEntregas()) {
        if (!donaciones.existeDonacion(pedido.getIdDonacion())) {
          ctx.status(HttpStatus.NOT_FOUND).result("No se pudo registrar la ruta");
          return;
        }
        ruta.agregarEntrega(new Entrega(pedido.getIdDonacion(), null,
            pedido.getRazonSocial(), pedido.getDireccion(), pedido.getTelefono(),
            pedido.getOrden()));
      }
    } catch (IOException e) {
      ctx.status(HttpStatus.BAD_GATEWAY).result("No se pudo consultar Donaciones");
      return;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      ctx.status(HttpStatus.SERVICE_UNAVAILABLE).result("Consulta a Donaciones interrumpida");
      return;
    }

    monitor.registrarRuta(ruta);
    camion.marcarNoDisponible();
    ctx.status(HttpStatus.CREATED).result("Ruta registrada");
  }

  public void iniciarRuta(Context ctx) {
    String patente = ctx.pathParam("patente");
    if (monitor.iniciarRuta(patente)) {
      ctx.status(HttpStatus.OK).result("Ruta iniciada");
    } else {
      ctx.status(HttpStatus.NOT_FOUND).result("No hay ruta para el camion: " + patente);
    }
  }
}
