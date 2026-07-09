package org.example.api.donaciones;

import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.Repositorios.RepositorioResultadosAlgoritmos;
import org.example.dominio.algoritmo.AlgoritmoMadre;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.NecesidadRecurrente;
import org.example.dominio.beneficiario.Periodicidad;
import org.example.dominio.catalogo.Estado;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EjecucionAlgoritmosServiceTest {
  private RepositorioDonaciones repositorioDonaciones;
  private RepositorioEntidadesBeneficiarias repositorioEntidades;
  private RepositorioResultadosAlgoritmos repositorioResultados;
  private RepositorioAsignacionesDonacion repositorioAsignaciones;

  @BeforeEach
  void setUp() {
    repositorioDonaciones = RepositorioDonaciones.getInstance();
    repositorioEntidades = RepositorioEntidadesBeneficiarias.getInstance();
    repositorioResultados = RepositorioResultadosAlgoritmos.getInstance();
    repositorioAsignaciones = RepositorioAsignacionesDonacion.getInstance();

    repositorioDonaciones.limpiar();
    repositorioEntidades.limpiar();
    repositorioResultados.limpiar();
    repositorioAsignaciones.limpiar();
  }

  @Test
  void ejecutaAlgoritmosSoloParaDonacionesDeDiasAnterioresEnDeposito() {
    Subcategoria alimentos = new Subcategoria("SUB-1", "Alimentos", TipoAtributo.NO_PERECEDERO);
    EntidadBeneficiaria comedor = entidadConNecesidad("Comedor", alimentos);
    Donacion donacionDeAyer = donacionConFecha(alimentos, LocalDate.now().minusDays(1));
    Donacion donacionDeHoy = donacionConFecha(alimentos, LocalDate.now());
    Donacion donacionYaAsignada = donacionConFecha(alimentos, LocalDate.now().minusDays(1));
    donacionYaAsignada.cambiarEstado(EstadoDonacion.ASIGNACION_REALIZADA, "Asignada antes de ejecutar algoritmos");

    repositorioEntidades.agregar(comedor);
    repositorioDonaciones.agregar(donacionDeAyer);
    repositorioDonaciones.agregar(donacionDeHoy);
    repositorioDonaciones.agregar(donacionYaAsignada);

    EjecucionAlgoritmosService service = new EjecucionAlgoritmosService(
        repositorioDonaciones,
        repositorioEntidades,
        repositorioResultados,
        new AlgoritmoMadre()
    );

    service.ejecutarAlgoritmosEnHorarioBajaCarga();

    assertEquals(1, repositorioResultados.buscarTodos().size());
    assertEquals(1, repositorioResultados.buscarPorDonacion(donacionDeAyer.getId()).size());
    assertEquals(0, repositorioResultados.buscarPorDonacion(donacionDeHoy.getId()).size());
    assertEquals(0, repositorioResultados.buscarPorDonacion(donacionYaAsignada.getId()).size());
  }

  @Test
  void puedeEjecutarseValidaFechaAnteriorAHoyYEstadoEnDeposito() {
    Subcategoria alimentos = new Subcategoria("SUB-1", "Alimentos", TipoAtributo.NO_PERECEDERO);
    Donacion donacionDeAyer = donacionConFecha(alimentos, LocalDate.now().minusDays(1));
    Donacion donacionDeHoy = donacionConFecha(alimentos, LocalDate.now());
    Donacion donacionYaAsignada = donacionConFecha(alimentos, LocalDate.now().minusDays(1));
    donacionYaAsignada.cambiarEstado(EstadoDonacion.ASIGNACION_REALIZADA, "Asignada antes de ejecutar algoritmos");
    EjecucionAlgoritmosService service = new EjecucionAlgoritmosService(
        repositorioDonaciones,
        repositorioEntidades,
        repositorioResultados,
        new AlgoritmoMadre()
    );

    assertTrue(service.puedeEjecutarse(donacionDeAyer));
    assertFalse(service.puedeEjecutarse(donacionDeHoy));
    assertFalse(service.puedeEjecutarse(donacionYaAsignada));
  }

  private Donacion donacionConFecha(Subcategoria subcategoria, LocalDate fechaDeRegistro) {
    return new Donacion(
        "Alimentos",
        20,
        "UNIDAD",
        subcategoria,
        fechaDeRegistro,
        null,
        Estado.NUEVO
    );
  }

  private EntidadBeneficiaria entidadConNecesidad(String razonSocial, Subcategoria subcategoria) {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(razonSocial, "Direccion", "Telefono");
    NecesidadRecurrente necesidad = new NecesidadRecurrente(
        "Necesidad " + razonSocial,
        20,
        subcategoria,
        LocalDate.now(),
        Periodicidad.SEMANAL
    );

    entidad.registrarNecesidad(necesidad);
    return entidad;
  }
}
