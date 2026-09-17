package org.example.api.logistica;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.api.logistica.dto.RutaRequest;
import org.example.Repositorios.RepositorioCamiones;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.dominio.logistica.MonitorCamiones;
import org.example.dominio.logistica.Ruta;
import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;
import org.example.dominio.notificacion.Notificador;

public class RutaController {

  private final MonitorCamiones monitor;
  private final Notificador notificador;

  public RutaController() {
    this(MonitorCamiones.getInstance(), new Notificador(new Email(null), new SMS(), new WhatsApp()));
  }

  public RutaController(MonitorCamiones monitor, Notificador notificador) {
    this.monitor = monitor;
    this.notificador = notificador;
  }

  public void registrarRuta(Context ctx) {
    RutaRequest request = ctx.bodyAsClass(RutaRequest.class);
    boolean registrada = monitor.registrarRuta(request.aPlan(), RepositorioCamiones.getInstance(),
        RepositorioDonaciones.getInstance()::buscarPorId);

    if (!registrada) {
      ctx.status(HttpStatus.NOT_FOUND).result("No se pudo registrar la ruta");
      return;
    }

    ctx.status(HttpStatus.CREATED).result("Ruta registrada");
  }

  public void iniciarRuta(Context ctx) {
    String patente = ctx.pathParam("patente");
    Ruta ruta = monitor.rutaDe(patente);
    if (ruta == null) {
      ctx.status(HttpStatus.NOT_FOUND).result("No hay ruta para el camion: " + patente);
      return;
    }
    try {
      ruta.iniciar(notificador);
      ctx.status(HttpStatus.OK).result("Ruta iniciada");
    } catch (IllegalStateException e) {
      ctx.status(HttpStatus.CONFLICT).result(e.getMessage());
    }
  }
}
