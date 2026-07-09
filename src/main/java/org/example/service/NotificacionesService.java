package org.example.service;

import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioRegistroDonacion;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Representante;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.RegistroDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.logistica.Entrega;
import org.example.dominio.logistica.Ruta;
import org.example.dominio.notificacion.Notificacion;
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

  //Esta tarea se ejecutará automáticamente todos los días a las 09:00 AM.
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

        // Si pusiéramos "> 20", el sistema le mandaría un mensaje todos los días de su vida a partir del día 21.
        if (diasInactivo == 21) {
          String mensaje = "¡Hola! Te extrañamos. Notamos que hace más de 20 días no registrás interacción en nuestra plataforma. ¿Te gustaría realizar una nueva donación y seguir ayudando?";

          notificador.notificarDonante(donante, mensaje);
        }
      }
    }
  }

  public void notificarInicioRuta(Ruta ruta) {
    for (Entrega entrega : ruta.getEntregas()) {
      notificador.notificarDonante(
              entrega.getDonacion().getDonante(),
              "Tu donación inició su recorrido. Podés seguir la entrega en el mapa interactivo."
      );
    }
  }

  public void notificarEntregaExitosa(Entrega entrega) {
    notificador.notificarDonante(
            entrega.getDonacion().getDonante(),
            "Tu donación fue entregada correctamente. Se generó el comprobante de entrega."
    );
  }

  public void notificarEntregaNoRecibida(Entrega entrega, String motivo) {
    notificador.notificarDonante(
            entrega.getDonacion().getDonante(),
            "No se pudo concretar la entrega de tu donación. Motivo: " + motivo
    );
  }

  // Evento "Donación asignada (beneficiario)": se notifica a la entidad
  // beneficiaria cuando se le asigna una donación en base a sus necesidades.
  public Notificacion notificarDonacionAsignadaBeneficiario(AsignacionDonacion asignacion) {
    EntidadBeneficiaria entidad = asignacion.getEntidad();
    String email = obtenerEmailDeContacto(entidad);

    if (email == null) {
      return null;
    }

    String mensaje = "Se le asignó una donación en base a sus necesidades. "
            + "Entidad: " + entidad.getRazonSocial() + ".";

    return notificador.notificarPorEmail(email, mensaje);
  }

  // Evento "Donación asignada (donante)": se notifica a la persona donante
  // cuando su donación acaba de ser asignada a una entidad beneficiaria.
  public Notificacion notificarDonacionAsignadaDonante(AsignacionDonacion asignacion) {
    Donante donante = asignacion.getDonacion().getDonante();

    String mensaje = "Tu donación acaba de ser asignada a la entidad beneficiaria "
            + asignacion.getEntidad().getRazonSocial() + ".";

    return notificador.notificarDonante(donante, mensaje);
  }

  private String obtenerEmailDeContacto(EntidadBeneficiaria entidad) {
    for (Representante representante : entidad.getRepresentantes()) {
      if (representante.getEmail() != null) {
        return representante.getEmail();
      }
    }

    return null;
  }
}
