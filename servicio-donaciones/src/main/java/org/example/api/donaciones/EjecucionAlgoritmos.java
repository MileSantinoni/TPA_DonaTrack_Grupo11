package org.example.api.donaciones;

import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.Repositorios.RepositorioResultadosAlgoritmos;
import org.example.dominio.algoritmo.AlgoritmoMadre;
import org.example.dominio.algoritmo.ResultadoEjecucionAlgoritmos;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;

import java.time.LocalDate;
import java.util.List;


public class EjecucionAlgoritmos {
  private final RepositorioDonaciones repositorioDonaciones;
  private final RepositorioEntidadesBeneficiarias repositorioEntidades;
  private final RepositorioResultadosAlgoritmos repositorioResultados;
  private final AlgoritmoMadre algoritmoMadre;

  public EjecucionAlgoritmos() {
    this(
        RepositorioDonaciones.getInstance(),
        RepositorioEntidadesBeneficiarias.getInstance(),
        RepositorioResultadosAlgoritmos.getInstance(),
        new AlgoritmoMadre()
    );
  }

  public EjecucionAlgoritmos(RepositorioDonaciones repositorioDonaciones,
                                    RepositorioEntidadesBeneficiarias repositorioEntidades,
                                    RepositorioResultadosAlgoritmos repositorioResultados,
                                    AlgoritmoMadre algoritmoMadre) {
    this.repositorioDonaciones = repositorioDonaciones;
    this.repositorioEntidades = repositorioEntidades;
    this.repositorioResultados = repositorioResultados;
    this.algoritmoMadre = algoritmoMadre;
  }
  public void ejecutarAlgoritmosEnHorarioBajaCarga() {
    List<Donacion> donacionesPendientes = buscarDonacionesPendientes();
    List<EntidadBeneficiaria> entidades = repositorioEntidades.buscarTodas();

    for (Donacion donacion : donacionesPendientes) {
      List<EntidadBeneficiaria> entidadesPropuestas = algoritmoMadre.proponer(donacion, entidades);

      repositorioResultados.eliminarPorDonacion(donacion.getId());
      repositorioResultados.agregar(new ResultadoEjecucionAlgoritmos(donacion, entidadesPropuestas));
    }
  }

  private List<Donacion> buscarDonacionesPendientes() {
    return repositorioDonaciones.buscarTodas().stream()
        .filter(this::puedeEjecutarse)
        .toList();
  }

  public boolean puedeEjecutarse(Donacion donacion) {
    return donacion.getEstadoActual() == EstadoDonacion.EN_DEPOSITO
        && donacion.getFechaDeRegistro().isBefore(LocalDate.now());
  }
}
