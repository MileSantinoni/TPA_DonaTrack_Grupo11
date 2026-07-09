package org.example.dominio.algoritmo;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.NecesidadRecurrente;
import org.example.dominio.beneficiario.Periodicidad;
import org.example.dominio.catalogo.Estado;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AlgoritmoCompatibilidadSemanticaTest {

  private Donante donanteDummy() {
    return new PersonaHumana(
        "donante@test.com",
        "12345678",
        TipoDocumento.DNI,
        "Donante",
        "Test",
        30,
        Genero.OTRO,
        "Direccion test"
    );
  }

  @Test
  void ordenaCandidaturasPorAjusteDeCantidadSinPuntaje() {
    Subcategoria papa = new Subcategoria("SUB-1", "Papa", TipoAtributo.NO_PERECEDERO);
    Subcategoria camas = new Subcategoria("SUB-2", "Camas", TipoAtributo.CON_ESTADO);

    Donacion donacionPapa = new Donacion(
        "Bolsa de papa",
        20,
        "KG",
        papa,
        null,
        Estado.NUEVO,
        donanteDummy()
    );

    EntidadBeneficiaria exacta = entidadConNecesidad("Entidad exacta", papa, 20);
    EntidadBeneficiaria sobraPoco = entidadConNecesidad("Entidad sobra poco", papa, 18);
    EntidadBeneficiaria sobraMucho = entidadConNecesidad("Entidad sobra mucho", papa, 10);
    EntidadBeneficiaria faltaPoco = entidadConNecesidad("Entidad falta poco", papa, 22);

    AlgoritmoCompatibilidadSemantica algoritmo = new AlgoritmoCompatibilidadSemantica();

    List<CandidaturaBeneficiaria> candidaturas = algoritmo.proponer(List.of(
        candidatura(donacionPapa, faltaPoco),
        candidatura(donacionPapa, sobraMucho),
        candidatura(donacionPapa, exacta),
        candidatura(donacionPapa, sobraPoco)
    ));

    assertEquals(4, candidaturas.size());
    assertEquals("Entidad exacta", candidaturas.get(0).getEntidad().getRazonSocial());
    assertEquals("Entidad sobra poco", candidaturas.get(1).getEntidad().getRazonSocial());
    assertEquals("Entidad sobra mucho", candidaturas.get(2).getEntidad().getRazonSocial());
    assertEquals("Entidad falta poco", candidaturas.get(3).getEntidad().getRazonSocial());
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

  private CandidaturaBeneficiaria candidatura(Donacion donacion, EntidadBeneficiaria entidad) {
    return new CandidaturaBeneficiaria(donacion, entidad, entidad.getNecesidades().get(0));
  }
}
