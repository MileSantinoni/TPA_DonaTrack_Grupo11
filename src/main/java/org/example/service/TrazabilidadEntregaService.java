package org.example.service;

import java.util.List;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.Entrega;
import org.example.dominio.logistica.EstadoEntrega;
import org.example.dominio.logistica.Ruta;
import org.example.service.NotificacionesService;
import org.springframework.stereotype.Service;

@Service
public class TrazabilidadEntregaService {

  private final NotificacionesService notificacionesService;

  public TrazabilidadEntregaService(NotificacionesService notificacionesService) {
    this.notificacionesService = notificacionesService;
  }

  public void iniciarRuta(Ruta ruta) {
    ruta.iniciar();

    for (Entrega entrega : ruta.getEntregas()) {
      if (entrega.getEstado() == EstadoEntrega.PENDIENTE) {
        entrega.marcarEnTraslado();

        entrega.getDonacion().cambiarEstado(
            EstadoDonacion.EN_TRASLADO,
            "El chofer inició la ruta"
        );

        notificacionesService.notificarInicioRuta(ruta);

      }
    }
  }

  public void confirmarRecepcion(Entrega entrega, Camion camion, List<String> fotos) {
    entrega.marcarEntregada(camion);

    for (String foto : fotos) {
      entrega.agregarFotoRecepcion(foto);
    }

    entrega.getDonacion().cambiarEstado(
        EstadoDonacion.ENTREGADA,
        "La entidad beneficiaria confirmó la recepción"
    );

    notificacionesService.notificarEntregaExitosa(entrega);
  }

  public void marcarNoRecibida(Entrega entrega, String motivo) {
    entrega.marcarNoRecibida();

    entrega.getDonacion().cambiarEstado(
        EstadoDonacion.ENTREGA_FALLIDA,
        motivo
    );

    notificacionesService.notificarEntregaNoRecibida(entrega, motivo);
  }

  public void registrarRetornoADeposito(Entrega entrega, String motivo) {
    entrega.volverAPendiente();

    entrega.getDonacion().cambiarEstado(
        EstadoDonacion.EN_DEPOSITO,
        motivo
    );
  }
}
