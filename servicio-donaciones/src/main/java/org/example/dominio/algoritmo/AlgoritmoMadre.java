package org.example.dominio.algoritmo;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Necesidad;
import org.example.dominio.donacion.Donacion;

import java.util.ArrayList;
import java.util.List;

public class AlgoritmoMadre {
  private AlgoritmoCompatibilidadSemantica algoritmoCompatibilidadSemantica;
  private AlgoritmoPrioridadSubAtendidos algoritmoPrioridadSubAtendidos;

  public AlgoritmoMadre() {
    this(new AlgoritmoCompatibilidadSemantica(), new AlgoritmoPrioridadSubAtendidos());
  }

  public AlgoritmoMadre(AlgoritmoCompatibilidadSemantica algoritmoCompatibilidadSemantica,
                        AlgoritmoPrioridadSubAtendidos algoritmoPrioridadSubAtendidos) {
    this.algoritmoCompatibilidadSemantica = algoritmoCompatibilidadSemantica;
    this.algoritmoPrioridadSubAtendidos = algoritmoPrioridadSubAtendidos;
  }

  public List<EntidadBeneficiaria> proponer(Donacion donacion, List<EntidadBeneficiaria> entidades) {
    List<CandidaturaBeneficiaria> candidaturas = buscarCandidaturas(donacion, entidades);
    List<EntidadBeneficiaria> entidadesPorCompatibilidad =
        obtenerEntidades(algoritmoCompatibilidadSemantica.proponer(candidaturas));
    List<EntidadBeneficiaria> entidadesSubAtendidas =
        obtenerEntidades(algoritmoPrioridadSubAtendidos.proponer(candidaturas));
    List<EntidadBeneficiaria> coincidencias = buscarCoincidencias(entidadesPorCompatibilidad, entidadesSubAtendidas);

    if (!coincidencias.isEmpty()) {
      return coincidencias;
    }

    return concatenarSinRepetidos(entidadesPorCompatibilidad, entidadesSubAtendidas);
  }

  private List<CandidaturaBeneficiaria> buscarCandidaturas(Donacion donacion,
                                                          List<EntidadBeneficiaria> entidades) {
    List<CandidaturaBeneficiaria> candidaturas = new ArrayList<>();

    for (EntidadBeneficiaria entidad : entidades) {
      for (Necesidad necesidad : entidad.getNecesidades()) {
        if (mismaSubcategoria(donacion, necesidad)) {
          candidaturas.add(new CandidaturaBeneficiaria(donacion, entidad, necesidad));
        }
      }
    }

    return candidaturas;
  }

  private List<EntidadBeneficiaria> obtenerEntidades(List<CandidaturaBeneficiaria> candidaturas) {
    List<EntidadBeneficiaria> entidades = new ArrayList<>();

    for (CandidaturaBeneficiaria candidatura : candidaturas) {
      agregarSiNoExiste(entidades, candidatura.getEntidad());
    }

    return entidades;
  }

  private boolean mismaSubcategoria(Donacion donacion, Necesidad necesidad) {
    return donacion.getSubcategoria().getId().equals(necesidad.getSubcategoria().getId());
  }

  private List<EntidadBeneficiaria> buscarCoincidencias(List<EntidadBeneficiaria> entidadesPorCompatibilidad,
                                                        List<EntidadBeneficiaria> entidadesSubAtendidas) {
    List<EntidadBeneficiaria> coincidencias = new ArrayList<>();

    for (EntidadBeneficiaria entidad : entidadesPorCompatibilidad) {
      if (contieneEntidad(entidadesSubAtendidas, entidad)) {
        coincidencias.add(entidad);
      }
    }

    return coincidencias;
  }

  private List<EntidadBeneficiaria> concatenarSinRepetidos(List<EntidadBeneficiaria> primeraLista,
                                                           List<EntidadBeneficiaria> segundaLista) {
    List<EntidadBeneficiaria> resultado = new ArrayList<>();

    for (EntidadBeneficiaria entidad : primeraLista) {
      agregarSiNoExiste(resultado, entidad);
    }

    for (EntidadBeneficiaria entidad : segundaLista) {
      agregarSiNoExiste(resultado, entidad);
    }

    return resultado;
  }

  private void agregarSiNoExiste(List<EntidadBeneficiaria> entidades, EntidadBeneficiaria entidad) {
    if (!contieneEntidad(entidades, entidad)) {
      entidades.add(entidad);
    }
  }

  private boolean contieneEntidad(List<EntidadBeneficiaria> entidades, EntidadBeneficiaria entidadBuscada) {
    for (EntidadBeneficiaria entidad : entidades) {
      if (entidad.getId().equals(entidadBuscada.getId())) {
        return true;
      }
    }

    return false;
  }
}
