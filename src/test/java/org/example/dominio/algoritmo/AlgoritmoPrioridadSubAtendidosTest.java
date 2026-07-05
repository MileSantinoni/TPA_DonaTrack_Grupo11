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

    EntidadBeneficiaria comedor = entidadConNecesidad("Comedor", alimentos);
    EntidadBeneficiaria escuela = entidadConNecesidad("Escuela", alimentos);
    EntidadBeneficiaria hogar = entidadConNecesidad("Hogar", alimentos);

    repositorio.agregar(asignacionPara(comedor, alimentos, hoy.minusDays(10)));
    repositorio.agregar(asignacionPara(comedor, alimentos, hoy.minusMonths(2)));
    repositorio.agregar(asignacionPara(escuela, alimentos, hoy.minusDays(5)));
    repositorio.agregar(asignacionPara(hogar, alimentos, hoy.minusMonths(4)));

    AlgoritmoPrioridadSubAtendidos algoritmo = new AlgoritmoPrioridadSubAtendidos(repositorio);

    List<EntidadBeneficiaria> propuestas = algoritmo.proponer(List.of(comedor, escuela, hogar));

    assertEquals(List.of(hogar, escuela, comedor), propuestas);
  }

  @Test
  void usaElRepositorioParaBuscarAsignacionesDelUltimoTrimestre() {
    LocalDate hoy = LocalDate.now();
    Subcategoria alimentos = new Subcategoria("SUB-1", "Alimentos", TipoAtributo.NO_PERECEDERO);
    RepositorioAsignacionesDonacion repositorio = RepositorioAsignacionesDonacion.getInstance();

    EntidadBeneficiaria comedor = entidadConNecesidad("Comedor", alimentos);
    EntidadBeneficiaria escuela = entidadConNecesidad("Escuela", alimentos);
    EntidadBeneficiaria hogar = entidadConNecesidad("Hogar", alimentos);

    repositorio.agregar(asignacionPara(comedor, alimentos, hoy.minusDays(7)));
    repositorio.agregar(asignacionPara(comedor, alimentos, hoy.minusMonths(2)));
    repositorio.agregar(asignacionPara(escuela, alimentos, hoy.minusDays(3)));
    repositorio.agregar(asignacionPara(hogar, alimentos, hoy.minusMonths(4)));

    AlgoritmoPrioridadSubAtendidos algoritmo = new AlgoritmoPrioridadSubAtendidos(repositorio);

    List<EntidadBeneficiaria> propuestas = algoritmo.proponer(List.of(comedor, escuela, hogar));

    assertEquals(List.of(hogar, escuela, comedor), propuestas);
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
    List<EntidadBeneficiaria> entidades = new ArrayList<>();

    for (int i = 1; i <= 12; i++) {
      entidades.add(new EntidadBeneficiaria("Entidad " + i, "Direccion", "Telefono"));
    }

    AlgoritmoPrioridadSubAtendidos algoritmo = new AlgoritmoPrioridadSubAtendidos();

    List<EntidadBeneficiaria> propuestas = algoritmo.proponer(entidades);

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
}
