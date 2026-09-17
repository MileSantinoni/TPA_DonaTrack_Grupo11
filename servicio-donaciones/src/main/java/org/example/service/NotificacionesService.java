package org.example.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioRegistroDonacion;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Representante;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.RegistroDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.notificacion.Notificacion;
import org.example.dominio.notificacion.Notificador;

public class NotificacionesService {
  private final RepositorioDonantes repoDonantes = RepositorioDonantes.getInstance();
  private final RepositorioRegistroDonacion repoRegistros = RepositorioRegistroDonacion.getInstance();
  private final Notificador notificador;

  public NotificacionesService(Notificador notificador) {
    this.notificador = notificador;
  }

  public void verificarAusenciaDonantes() {
    LocalDate hoy = LocalDate.now();
    List<RegistroDonacion> registros = repoRegistros.obtenerTodos();
    for (Donante donante : repoDonantes.buscarTodos()) {
      Optional<LocalDate> ultima = registros.stream()
          .filter(r -> r.getDonante().getId().equals(donante.getId()))
          .map(RegistroDonacion::getFechaDeRegistro)
          .max(LocalDate::compareTo);
      if (ultima.isPresent() && ChronoUnit.DAYS.between(ultima.get(), hoy) == 21) {
        notificador.notificarDonante(donante,
            "Hola! Te extranamos. Notamos que hace mas de 20 dias no registras interaccion en nuestra plataforma. Te gustaria realizar una nueva donacion y seguir ayudando?");
      }
    }
  }

  public Notificacion notificarDonacionAsignadaBeneficiario(AsignacionDonacion asignacion) {
    EntidadBeneficiaria entidad = asignacion.getEntidad();
    for (Representante representante : entidad.getRepresentantes()) {
      if (representante.getEmail() != null && !representante.getEmail().isBlank()) {
        return notificador.notificarPorEmail(representante.getEmail(),
            "Se le asigno una donacion en base a sus necesidades. Entidad: "
                + entidad.getRazonSocial() + ".");
      }
    }
    return null;
  }

  public Notificacion notificarDonacionAsignadaDonante(AsignacionDonacion asignacion) {
    return notificador.notificarDonante(asignacion.getDonacion().getDonante(),
        "Tu donacion acaba de ser asignada a la entidad beneficiaria "
            + asignacion.getEntidad().getRazonSocial() + ".");
  }
}
