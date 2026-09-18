package org.example.dominio.beneficiario.test;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.NecesidadExtraordinaria;
import org.example.dominio.beneficiario.NecesidadRecurrente;
import org.example.dominio.beneficiario.Periodicidad;
import org.example.dominio.beneficiario.Representante;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;

public class BeneficiariosYNecesidadesTest {

  private Subcategoria subcategoriaSillas;
  private Subcategoria subcategoriaFideos;
  private EntidadBeneficiaria escuelaRural;
  private EntidadBeneficiaria comedorInfantil;

  @BeforeEach
  public void setUp() {
    // Inicializamos las subcategorías necesarias
    subcategoriaSillas = new Subcategoria("SUB-1", "sillas", TipoAtributo.CON_ESTADO);
    subcategoriaFideos = new Subcategoria("SUB-2", "fideos secos", TipoAtributo.PERECEDERO);

    // Inicializamos las entidades beneficiarias
    escuelaRural = new EntidadBeneficiaria("Escuela Rural N°10", "Ruta 4 Km 20", "555-0101");
    comedorInfantil = new EntidadBeneficiaria("Comedor Escobar Sonrisas", "Calle Falsa 123", "555-0202");
  }

  @Test
  public void testRegistroEntidadYRepresentante() {
    // Creamos y asignamos un representante a la entidad
    Representante director = new Representante("Juan", "Pérez", "juan.perez@escuela10.edu.ar");
    escuelaRural.agregarRepresentante(director);

    // Verificamos que la entidad se haya creado bien y contenga a su representante
    assertEquals("Escuela Rural N°10", escuelaRural.getRazonSocial());
    assertEquals(1, escuelaRural.getRepresentantes().size());
    assertEquals("juan.perez@escuela10.edu.ar", escuelaRural.getRepresentantes().get(0).getEmail());
  }

  @Test
  public void testNecesidadExtraordinariaDonacionesParciales() {
    // Caso de uso: La escuela rural N°10, tras una inundación, necesita 30 sillas.
    NecesidadExtraordinaria necesidadSillas = new NecesidadExtraordinaria(
        "Reposición de mobiliario por inundación en un aula",
        30,
        subcategoriaSillas,
        "Inundación"
    );
    escuelaRural.registrarNecesidad(necesidadSillas);

    // Inicialmente la necesidad NO debe estar satisfecha
    assertFalse(necesidadSillas.estaSatisfecha(), "La necesidad no debería estar satisfecha al crearse");

    // Una persona dona 2 sillas
    necesidadSillas.registrarDonacion(2);
    assertFalse(necesidadSillas.estaSatisfecha(), "Con 2 sillas donadas de 30, la necesidad no está satisfecha");

    // Otra persona dona 28 sillas (alcanzando el objetivo de 30)
    necesidadSillas.registrarDonacion(28);

    // Ahora la necesidad SÍ debe figurar como satisfecha
    assertTrue(necesidadSillas.estaSatisfecha(), "Al alcanzar las 30 sillas, la necesidad debe estar satisfecha");
  }

  @Test
  public void testNecesidadRecurrenteDonacionesParciales() {
    // Caso de uso: El comedor requiere 100 paquetes de fideos por semana
    NecesidadRecurrente necesidadFideos = new NecesidadRecurrente(
        "Abastecimiento habitual de fideos",
        100,
        subcategoriaFideos,
        LocalDate.now(),
        Periodicidad.SEMANAL
    );
    comedorInfantil.registrarNecesidad(necesidadFideos);

    // Inicialmente la necesidad NO debe estar satisfecha
    assertFalse(necesidadFideos.estaSatisfecha());

    // Se reciben 40 paquetes a principios de la semana
    necesidadFideos.registrarDonacion(40);
    assertFalse(necesidadFideos.estaSatisfecha(), "Con 40 paquetes donados de 100, no está satisfecha");

    // Se reciben 70 paquetes más (total 110). Se supera el objetivo.
    necesidadFideos.registrarDonacion(70);

    // Se considera satisfecha cuando se recibe una cantidad de bienes igualando o superando la cantidad requerida
    assertTrue(necesidadFideos.estaSatisfecha(), "Al superar los 100 paquetes, la necesidad semanal se satisface");
  }
}