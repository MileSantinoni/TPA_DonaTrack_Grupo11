package org.example.api.logistica;

import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import java.io.IOException;
import java.util.List;
import org.example.Repositorios.RepositorioRutas;
import org.example.dominio.logistica.*;

public class EntregaController {

  private final MonitorCamiones monitor;
  private final Donaciones donaciones;
  private final RepositorioRutas rutas;

  public EntregaController(
      MonitorCamiones monitor,
      Donaciones donaciones,
      RepositorioRutas rutas
  ) {
    this.monitor = monitor;
    this.donaciones = donaciones;
    this.rutas = rutas;
  }

  public void confirmarRecepcion(Context ctx)
      throws IOException, InterruptedException {

    RecepcionRequest request = ctx.bodyAsClass(RecepcionRequest.class);
    List<String> fotos =
        request.fotos() == null ? List.of() : request.fotos();

    if (fotos.stream().anyMatch(f -> f == null || f.isBlank())) {
      throw new IllegalArgumentException("Las fotos no pueden estar vacias");
    }

    Ruta ruta = ruta(ctx);
    Entrega entrega = entrega(ctx, ruta);

    ejecutar(
        ruta,
        () -> {
          entrega.prepararRecepcion(ruta.getCamion());
        },
        () -> {
          entrega.confirmarRecepcion(ruta.getCamion(), donaciones);
          fotos.forEach(entrega::agregarFotoRecepcion);
        }
    );

    ctx.status(204);
  }

  public void marcarNoRecibida(Context ctx)
      throws IOException, InterruptedException {

    MotivoRequest request = ctx.bodyAsClass(MotivoRequest.class);
    Ruta ruta = ruta(ctx);
    Entrega entrega = entrega(ctx, ruta);

    ejecutar(
        ruta,
        () -> {
          entrega.prepararNoRecibida(request.motivo(), ruta.getCamion());
        },
        () -> entrega.marcarNoRecibida(
            request.motivo(), ruta.getCamion(), donaciones
        )
    );

    ctx.status(204);
  }

  public void registrarRetorno(Context ctx)
      throws IOException, InterruptedException {

    MotivoRequest request = ctx.bodyAsClass(MotivoRequest.class);
    Ruta ruta = ruta(ctx);
    Entrega entrega = entrega(ctx, ruta);

    ejecutar(
        ruta,
        () -> {
          entrega.prepararRetorno(request.motivo(), ruta.getCamion());
        },
        () -> entrega.registrarRetornoADeposito(
            request.motivo(), ruta.getCamion(), donaciones
        )
    );

    ctx.status(204);
  }

  private void ejecutar(
      Ruta ruta,
      Runnable preparar,
      OperacionEntrega operacion
  ) throws IOException, InterruptedException {

    // Primera transacción: conservar el evento antes del HTTP.
    rutas.enTransaccion(() -> {
      preparar.run();
      rutas.actualizar(ruta);
    });

    // Comunicación HTTP sin mantener una transacción abierta.
    try {
      operacion.ejecutar();
    } catch (IllegalArgumentException | IllegalStateException rechazo) {
      // El dominio elimina el evento ante un rechazo definitivo.
      // Guardamos también esa eliminación.
      rutas.actualizar(ruta);
      throw rechazo;
    }

    // Si hubo IOException o InterruptedException, no llegamos acá:
    // la base conserva el evento pendiente para el reintento.

    // Segunda transacción: estados, fecha, fotos y eliminación del evento.
    rutas.actualizar(ruta);
  }

  private Ruta ruta(Context ctx) {
    Ruta ruta = monitor.rutaDe(ctx.pathParam("patente"));

    if (ruta == null) {
      throw new NotFoundResponse("No hay ruta para el camion");
    }

    return ruta;
  }

  private Entrega entrega(Context ctx, Ruta ruta) {
    return ruta.getEntregas().stream()
        .filter(e -> e.getIdDonacion().equals(ctx.pathParam("idDonacion")))
        .findFirst()
        .orElseThrow(
            () -> new NotFoundResponse("No existe la entrega en esa ruta")
        );
  }

  @FunctionalInterface
  private interface OperacionEntrega {
    void ejecutar() throws IOException, InterruptedException;
  }

  public record RecepcionRequest(List<String> fotos) {}

  public record MotivoRequest(String motivo) {}
}