package org.example.api.notificaciones;

import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioRegistroDonacion;
import org.example.dominio.donacion.RegistroDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.notificacion.Notificador;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class NotificadorAusenciaService {

  private final RepositorioDonantes repoDonantes = RepositorioDonantes.getInstance();
  private final RepositorioRegistroDonacion repoRegistros = RepositorioRegistroDonacion.getInstance();


  private final Notificador notificador = new Notificador();

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
}
