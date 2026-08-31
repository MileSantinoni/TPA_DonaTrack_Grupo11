package org.example.api.logistica;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.api.logistica.dto.RutaRequest;
import org.example.service.RutaService;

public class RutaController {

  private final RutaService rutaService;

  public RutaController() {
    this(new RutaService());
  }

  public RutaController(RutaService rutaService) {
    this.rutaService = rutaService;
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
    boolean iniciada = rutaService.iniciarRuta(patente);

    if (iniciada) {
      ctx.status(HttpStatus.OK).result("Ruta iniciada");
    } else {
      ctx.status(HttpStatus.NOT_FOUND).result("No hay ruta para el camion: " + patente);
    }
  }
}