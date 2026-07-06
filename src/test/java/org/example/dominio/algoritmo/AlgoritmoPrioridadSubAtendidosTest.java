package org.example.dominio.algoritmo;

import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.NecesidadRecurrente;
import org.example.dominio.beneficiario.Periodicidad;
import org.example.dominio.catalogo.Estado;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.Donacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlgoritmoPrioridadSubAtendidosTest {

  @BeforeEach
  void limpiarRepositorio() {
    RepositorioAsignacionesDonacion.getInstance().limpiar();
  }

  @Test
  void priorizaEntidadesConMenosRecepcionesEnElUltimoTrimestre() {
    LocalDate hoy = LocalDate.now();
    Subcategoria alimentos = new Subcategoria("SUB-1", "Alimentos", TipoAtributo.NO_PERECEDERO);
    RepositorioAsignacionesDonacion repositorio = RepositorioAsignacionesDonacion.getInstance();
    Donacion donacionAlimentos = new Donacion("Alimentos", 1, "UNIDAD", alimentos, null, Estado.NUEVO);

    EntidadBeneficiaria comedor = entidadConNecesidad("Comedor", alimentos);
    EntidadBeneficiaria escuela = entidadConNecesidad("Escuela", alimentos);
    EntidadBeneficiaria hogar = entidadConNecesidad("Hogar", alimentos);

    repositorio.agregar(asignacionPara(comedor, alimentos, hoy.minusDays(10)));
    repositorio.agregar(asignacionPara(comedor, alimentos, hoy.minusMonths(2)));
    repositorio.agregar(asignacionPara(escuela, alimentos, hoy.minusDays(5)));
    repositorio.agregar(asignacionPara(hogar, alimentos, hoy.minusMonths(4)));

    AlgoritmoPrioridadSubAtendidos algoritmo = new AlgoritmoPrioridadSubAtendidos(repositorio);

    List<CandidaturaBeneficiaria> propuestas = algoritmo.proponer(candidaturasPara(
        donacionAlimentos,
        List.of(comedor, escuela, hogar)
    ));

    assertEquals(List.of(hogar, escuela, comedor), entidadesDe(propuestas));
  }

  @Test
  void usaElRepositorioParaBuscarAsignacionesDelUltimoTrimestre() {
    LocalDate hoy = LocalDate.now();
    Subcategoria alimentos = new Subcategoria("SUB-1", "Alimentos", TipoAtributo.NO_PERECEDERO);
    RepositorioAsignacionesDonacion repositorio = RepositorioAsignacionesDonacion.getInstance();
    Donacion donacionAlimentos = new Donacion("Alimentos", 1, "UNIDAD", alimentos, null, Estado.NUEVO);

    EntidadBeneficiaria comedor = entidadConNecesidad("Comedor", alimentos);
    EntidadBeneficiaria escuela = entidadConNecesidad("Escuela", alimentos);
    EntidadBeneficiaria hogar = entidadConNecesidad("Hogar", alimentos);

    repositorio.agregar(asignacionPara(comedor, alimentos, hoy.minusDays(7)));
    repositorio.agregar(asignacionPara(comedor, alimentos, hoy.minusMonths(2)));
    repositorio.agregar(asignacionPara(escuela, alimentos, hoy.minusDays(3)));
    repositorio.agregar(asignacionPara(hogar, alimentos, hoy.minusMonths(4)));

    AlgoritmoPrioridadSubAtendidos algoritmo = new AlgoritmoPrioridadSubAtendidos(repositorio);

    List<CandidaturaBeneficiaria> propuestas = algoritmo.proponer(candidaturasPara(
        donacionAlimentos,
        List.of(comedor, escuela, hogar)
    ));

    assertEquals(List.of(hogar, escuela, comedor), entidadesDe(propuestas));
  }

  @Test
  void repositorioBuscaAsignacionesEntreFechas() {
    LocalDate hoy = LocalDate.now();
    Subcategoria alimentos = new Subcategoria("SUB-1", "Alimentos", TipoAtributo.NO_PERECEDERO);
    RepositorioAsignacionesDonacion repositorio = RepositorioAsignacionesDonacion.getInstance();
    EntidadBeneficiaria comedor = entidadConNecesidad("Comedor", alimentos);

    repositorio.agregar(asignacionPara(comedor, alimentos, hoy.minusDays(10)));
    repositorio.agregar(asignacionPara(comedor, alimentos, hoy.minusMonths(2)));
    repositorio.agregar(asignacionPara(comedor, alimentos, hoy.minusMonths(4)));

    List<AsignacionDonacion> asignacionesUltimoTrimestre =
        repositorio.buscarEntreFechas(hoy.minusMonths(3), hoy);

    assertEquals(2, asignacionesUltimoTrimestre.size());
  }

  @Test
  void proponeComoMaximoDiezEntidades() {
    Subcategoria alimentos = new Subcategoria("SUB-1", "Alimentos", TipoAtributo.NO_PERECEDERO);
    Donacion donacionAlimentos = new Donacion("Alimentos", 1, "UNIDAD", alimentos, null, Estado.NUEVO);
    List<EntidadBeneficiaria> entidades = new ArrayList<>();

    for (int i = 1; i <= 12; i++) {
      entidades.add(entidadConNecesidad("Entidad " + i, alimentos));
    }

    AlgoritmoPrioridadSubAtendidos algoritmo = new AlgoritmoPrioridadSubAtendidos();

    List<CandidaturaBeneficiaria> propuestas = algoritmo.proponer(candidaturasPara(donacionAlimentos, entidades));

    assertEquals(10, propuestas.size());
  }

  private AsignacionDonacion asignacionPara(EntidadBeneficiaria entidad, Subcategoria subcategoria, LocalDate fecha) {
    Donacion donacion = new Donacion("Alimentos", 1, "UNIDAD", subcategoria, null, Estado.NUEVO);
    return new AsignacionDonacion(donacion, entidad, entidad.getNecesidades().get(0), fecha);
  }

  private EntidadBeneficiaria entidadConNecesidad(String razonSocial, Subcategoria subcategoria) {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(razonSocial, "Direccion", "Telefono");
    NecesidadRecurrente necesidad = new NecesidadRecurrente(
        "Necesidad " + razonSocial,
        10,
        subcategoria,
        LocalDate.now(),
        Periodicidad.SEMANAL
    );

    entidad.registrarNecesidad(necesidad);
    return entidad;
  }

  private List<EntidadBeneficiaria> entidadesDe(List<CandidaturaBeneficiaria> candidaturas) {
    List<EntidadBeneficiaria> entidades = new ArrayList<>();

    for (CandidaturaBeneficiaria candidatura : candidaturas) {
      entidades.add(candidatura.getEntidad());
    }

    return entidades;
  }

  private List<CandidaturaBeneficiaria> candidaturasPara(Donacion donacion, List<EntidadBeneficiaria> entidades) {
    List<CandidaturaBeneficiaria> candidaturas = new ArrayList<>();

    for (EntidadBeneficiaria entidad : entidades) {
      candidaturas.add(new CandidaturaBeneficiaria(donacion, entidad, entidad.getNecesidades().get(0)));
    }

    return candidaturas;
  }
}
