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

public class AlgoritmoMadreTest {
  private RepositorioAsignacionesDonacion repositorio;

  @BeforeEach
  void setUp() {
    repositorio = RepositorioAsignacionesDonacion.getInstance();
    repositorio.limpiar();
  }

  @Test
  void devuelveEntidadesQueAparecenEnAmbosAlgoritmos() {
    Subcategoria papa = new Subcategoria("SUB-1", "Papa", TipoAtributo.NO_PERECEDERO);
    Donacion donacionPapa = new Donacion("Bolsa de papa", 20, "KG", papa, null, Estado.NUEVO);
    EntidadBeneficiaria exacta = entidadConNecesidad("Entidad exacta", papa, 20);
    EntidadBeneficiaria sobraPoco = entidadConNecesidad("Entidad sobra poco", papa, 18);
    EntidadBeneficiaria faltaPoco = entidadConNecesidad("Entidad falta poco", papa, 25);

    registrarRecepciones(sobraPoco, papa, 2);
    registrarRecepciones(faltaPoco, papa, 1);

    AlgoritmoMadre algoritmo = new AlgoritmoMadre(
        new AlgoritmoCompatibilidadSemantica(),
        new AlgoritmoPrioridadSubAtendidos(repositorio)
    );

    List<EntidadBeneficiaria> propuestas = algoritmo.proponer(
        donacionPapa,
        List.of(sobraPoco, faltaPoco, exacta)
    );

    assertEquals(List.of(exacta, sobraPoco, faltaPoco), propuestas);
  }

  @Test
  void siNoHayEntidadesEnComunDevuelveAmbasListasConcatenadas() {
    Subcategoria papa = new Subcategoria("SUB-1", "Papa", TipoAtributo.NO_PERECEDERO);
    Donacion donacionPapa = new Donacion("Bolsa de papa", 20, "KG", papa, null, Estado.NUEVO);
    List<EntidadBeneficiaria> entidades = new ArrayList<>();

    for (int i = 1; i <= 10; i++) {
      EntidadBeneficiaria compatibleMuyAtendida = entidadConNecesidad("Compat " + i, papa, 20);
      registrarRecepciones(compatibleMuyAtendida, papa, 20);
      entidades.add(compatibleMuyAtendida);
    }

    for (int i = 1; i <= 10; i++) {
      entidades.add(entidadConNecesidad("SubAtendida " + i, papa, 100));
    }

    AlgoritmoMadre algoritmo = new AlgoritmoMadre(
        new AlgoritmoCompatibilidadSemantica(),
        new AlgoritmoPrioridadSubAtendidos(repositorio)
    );

    List<EntidadBeneficiaria> propuestas = algoritmo.proponer(donacionPapa, entidades);

    assertEquals(20, propuestas.size());
    assertEquals("Compat 1", propuestas.get(0).getRazonSocial());
    assertEquals("SubAtendida 1", propuestas.get(10).getRazonSocial());
  }

  @Test
  void filtraNecesidadesPorSubcategoriaAntesDeEjecutarLosAlgoritmos() {
    Subcategoria papa = new Subcategoria("SUB-1", "Papa", TipoAtributo.NO_PERECEDERO);
    Subcategoria camas = new Subcategoria("SUB-2", "Camas", TipoAtributo.CON_ESTADO);
    Donacion donacionPapa = new Donacion("Bolsa de papa", 20, "KG", papa, null, Estado.NUEVO);
    EntidadBeneficiaria comedor = entidadConNecesidad("Comedor", papa, 20);
    EntidadBeneficiaria hogar = entidadConNecesidad("Hogar", camas, 20);

    AlgoritmoMadre algoritmo = new AlgoritmoMadre(
        new AlgoritmoCompatibilidadSemantica(),
        new AlgoritmoPrioridadSubAtendidos(repositorio)
    );

    List<EntidadBeneficiaria> propuestas = algoritmo.proponer(donacionPapa, List.of(comedor, hogar));

    assertEquals(1, propuestas.size());
    assertEquals(comedor, propuestas.get(0));
  }

  private void registrarRecepciones(EntidadBeneficiaria entidad, Subcategoria subcategoria, int cantidad) {
    for (int i = 0; i < cantidad; i++) {
      Donacion donacion = new Donacion("Donacion", 1, "UNIDAD", subcategoria, null, Estado.NUEVO);
      AsignacionDonacion asignacion = new AsignacionDonacion(
          donacion,
          entidad,
          entidad.getNecesidades().get(0),
          LocalDate.now().minusDays(1)
      );

      repositorio.agregar(asignacion);
    }
  }

  private EntidadBeneficiaria entidadConNecesidad(String razonSocial, Subcategoria subcategoria, int cantidadObjetivo) {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(razonSocial, "Direccion", "Telefono");
    NecesidadRecurrente necesidad = new NecesidadRecurrente(
        "Necesidad " + razonSocial,
        cantidadObjetivo,
        subcategoria,
        LocalDate.now(),
        Periodicidad.SEMANAL
    );

    entidad.registrarNecesidad(necesidad);
    return entidad;
  }
}
