package org.example.api.logistica;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.api.logistica.dto.RutaRequest;
import org.example.service.RutaService;
import org.example.dominio.logistica.MonitorCamiones;
import org.example.dominio.logistica.Ruta;
import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;
import org.example.dominio.notificacion.Notificador;

public class RutaController {

  private final RutaService rutaService;
  private final MonitorCamiones monitor;
  private final Notificador notificador;

  public RutaController() {
    this(new RutaService());
  }

  public RutaController(RutaService rutaService) {
    this(rutaService, MonitorCamiones.getInstance(), new Notificador(new Email(null), new SMS(), new WhatsApp()));
  }

  public RutaController(RutaService rutaService, MonitorCamiones monitor, Notificador notificador) {
    this.rutaService = rutaService;
    this.monitor = monitor;
    this.notificador = notificador;
  }

  public void registrarRuta(Context ctx) {
    RutaRequest request = ctx.bodyAsClass(RutaRequest.class);
    boolean registrada = rutaService.registrarRuta(request);

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
