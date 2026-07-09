package org.example.service;

import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioRegistroDonacion;
import org.example.dominio.donacion.RegistroDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.logistica.Entrega;
import org.example.dominio.logistica.Ruta;
import org.example.dominio.notificacion.Notificador;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class NotificacionesService {

  private final RepositorioDonantes repoDonantes = RepositorioDonantes.getInstance();
  private final RepositorioRegistroDonacion repoRegistros = RepositorioRegistroDonacion.getInstance();

  private final Notificador notificador;

  public NotificacionesService(Notificador notificador) {
    this.notificador = notificador;
  }

  // Esta tarea se ejecuta automaticamente todos los dias a las 09:00 AM.
  @Scheduled(cron = "0 0 9 * * ?")
  public void verificarAusenciaDonantes() {
    LocalDate hoy = LocalDate.now();
    List<Donante> todosLosDonantes = repoDonantes.buscarTodos();
    List<RegistroDonacion> todosLosRegistros = repoRegistros.obtenerTodos();

    for (Donante donante : todosLosDonantes) {

      Optional<LocalDate> ultimaFechaDeInteraccion = todosLosRegistros.stream()
          .filter(r -> r.getDonante().getId().equals(donante.getId()))
          .map(RegistroDonacion::getFechaDeRegistro)
          .max(LocalDate::compareTo);

      if (ultimaFechaDeInteraccion.isPresent()) {

        long diasInactivo = ChronoUnit.DAYS.between(ultimaFechaDeInteraccion.get(), hoy);

        // Si usaramos "> 20", se mandaria un mensaje todos los dias a partir del dia 21.
        if (diasInactivo == 21) {
          String mensaje = "Hola! Te extranamos. Notamos que hace mas de 20 dias no registras interaccion en nuestra plataforma. Te gustaria realizar una nueva donacion y seguir ayudando?";

          notificador.notificarDonante(donante, mensaje);
        }
      }
    }
  }

  public void notificarInicioRuta(Ruta ruta) {
    for (Entrega entrega : ruta.getEntregas()) {
      notificador.notificarEntidadBeneficiaria(
          entrega.getDestino(),
          "Tu envio ya esta en camino. Podes seguirlo en el mapa interactivo."
      );
      notificador.notificarDonante(
          entrega.getDonacion().getDonante(),
          "Tu donacion ya esta en camino. Podes seguir el envio en el mapa interactivo."
      );
    }
  }

  public void notificarEntregaExitosa(Entrega entrega) {
    notificador.notificarEntidadBeneficiaria(
        entrega.getDestino(),
        "El envio fue recibido correctamente. Se genero el comprobante de entrega."
    );
    notificador.notificarDonante(
        entrega.getDonacion().getDonante(),
        "Tu donacion fue entregada correctamente. Se genero el comprobante de entrega."
    );
  }

  public void notificarEntregaNoRecibida(Entrega entrega, String motivo) {
    notificador.notificarEntidadBeneficiaria(
        entrega.getDestino(),
        "No se pudo concretar la recepcion del envio. Motivo: " + motivo
    );
    notificador.notificarDonante(
        entrega.getDonacion().getDonante(),
        "No se pudo concretar la entrega de tu donacion. Motivo: " + motivo
    );
  }
}
