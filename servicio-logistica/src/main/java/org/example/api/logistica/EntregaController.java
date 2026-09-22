package org.example.api.logistica;

import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import java.util.List;
import org.example.dominio.logistica.*;

public class EntregaController {
  private final MonitorCamiones monitor;
  private final Donaciones donaciones;

  public EntregaController(MonitorCamiones monitor, Donaciones donaciones) {
    this.monitor = monitor;
    this.donaciones = donaciones;
  }

  public void confirmarRecepcion(Context ctx) throws java.io.IOException, InterruptedException {
    RecepcionRequest request = ctx.bodyAsClass(RecepcionRequest.class);
    List<String> fotos = request.fotos() == null ? List.of() : request.fotos();
    if (fotos.stream().anyMatch(f -> f == null || f.isBlank())) {
      throw new IllegalArgumentException("Las fotos no pueden estar vacias");
    }
    Ruta ruta = ruta(ctx);
    Entrega entrega = entrega(ctx, ruta);
    entrega.confirmarRecepcion(ruta.getCamion(), donaciones);
    fotos.forEach(entrega::agregarFotoRecepcion);
    ctx.status(204);
  }

  public void marcarNoRecibida(Context ctx) throws java.io.IOException, InterruptedException {
    MotivoRequest request = ctx.bodyAsClass(MotivoRequest.class);
    Ruta ruta = ruta(ctx);
    entrega(ctx, ruta).marcarNoRecibida(request.motivo(), ruta.getCamion(), donaciones);
    ctx.status(204);
  }

  public void registrarRetorno(Context ctx) throws java.io.IOException, InterruptedException {
    MotivoRequest request = ctx.bodyAsClass(MotivoRequest.class);
    Ruta ruta = ruta(ctx);
    entrega(ctx, ruta).registrarRetornoADeposito(request.motivo(), ruta.getCamion(), donaciones);
    ctx.status(204);
  }

  private Ruta ruta(Context ctx) {
    Ruta ruta = monitor.rutaDe(ctx.pathParam("patente"));
    if (ruta == null) throw new NotFoundResponse("No hay ruta para el camion");
    return ruta;
  }

  private Entrega entrega(Context ctx, Ruta ruta) {
    return ruta.getEntregas().stream().filter(e -> e.getIdDonacion().equals(ctx.pathParam("idDonacion")))
        .findFirst().orElseThrow(() -> new NotFoundResponse("No existe la entrega en esa ruta"));
  }

  public record RecepcionRequest(List<String> fotos) {}
  public record MotivoRequest(String motivo) {}
}
