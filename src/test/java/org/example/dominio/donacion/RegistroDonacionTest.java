package org.example.dominio.donacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.example.dominio.catalogo.Bien;
import org.example.dominio.catalogo.BienConEstado;
import org.example.dominio.catalogo.BienEstandar;
import org.example.dominio.catalogo.BienPerecedero;
import org.example.dominio.catalogo.Estado;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;

public class RegistroDonacionTest {

  @Test
  void unRegistroDeDonacionPuedeSegmentarSusBienesEnDonaciones() {
    Donante donante = new PersonaHumana(
        "pedro@mail.com",
        "12345678",
        TipoDocumento.DNI,
        "Pedro",
        "Gonzales",
        25,
        Genero.MASCULINO,
        "Av. Libertador 1234"
    );

    RegistroDonacion registro = new RegistroDonacion(
        "1",
        "Donación de muebles y alimentos",
        donante
    );

    Subcategoria silla = new Subcategoria("Silla");
    Subcategoria mesa = new Subcategoria("Mesa");

    Bien bien1 = new BienConEstado(
        "Sillas usadas",
        6,
        "UNIDAD",
        silla,
        Estado.USADO
    );

    Bien bien2 = new BienConEstado(
        "Mesa rectangular usada",
        1,
        "UNIDAD",
        mesa,
        Estado.USADO
    );

    registro.agregarBien(bien1);
    registro.agregarBien(bien2);

    List<Donacion> donaciones = registro.segmentar();

    assertEquals(2, donaciones.size());

    assertEquals("Sillas usadas", donaciones.get(0).getDescripcionGeneral());
    assertEquals(6, donaciones.get(0).getCantidad());
    assertEquals(silla, donaciones.get(0).getSubcategoria());

    assertEquals("Mesa rectangular usada", donaciones.get(1).getDescripcionGeneral());
    assertEquals(1, donaciones.get(1).getCantidad());
    assertEquals(mesa, donaciones.get(1).getSubcategoria());


  }

  @Test
  void unaDonacionResultanteIniciaEnEstadoDeposito() {
    Donante donante = new PersonaHumana(
        "ana@mail.com",
        "87654321",
        TipoDocumento.DNI,
        "Ana",
        "Perez",
        30,
        Genero.FEMENINO,
        "Av. Corrientes 1500"
    );

    RegistroDonacion registro = new RegistroDonacion(
        "2",
        "Donación de alimentos",
        donante
    );

    Subcategoria fideos = new Subcategoria( "Fideos");

    Bien bien = new BienEstandar(
        "Paquetes de fideos",
        100,
        "PAQUETE",
        fideos
    );

    registro.agregarBien(bien);

    List<Donacion> donaciones = registro.segmentar();

    assertEquals(1, donaciones.size());
    assertEquals(EstadoDonacion.EN_DEPOSITO, donaciones.get(0).getEstadoActual());
  }

  @Test
  void unaDonacionPerecederaConservaSuFechaDeVencimiento() {
    Donante donante = new PersonaHumana(
        "sofia@mail.com",
        "33444555",
        TipoDocumento.DNI,
        "Sofia",
        "Ramirez",
        29,
        Genero.FEMENINO,
        "Av. Santa Fe 2500"
    );

    RegistroDonacion registro = new RegistroDonacion(
        "3",
        "Donación de alimentos perecederos",
        donante
    );

    Subcategoria pureTomate = new Subcategoria( "Puré de tomate");

    Bien bien = new BienPerecedero(
        "Puré de tomate",
        50,
        "PAQUETE",
        pureTomate,
        LocalDate.of(2027, 1, 1)
    );

    registro.agregarBien(bien);

    List<Donacion> donaciones = registro.segmentar();

    assertEquals(1, donaciones.size());
    assertEquals(LocalDate.of(2027, 1, 1), donaciones.get(0).getFechaVencimiento());
  }

  @Test
  void alSegmentarUnBienUsadoLaDonacionResultanteConservaLaCondicionUsado() {
    Donante donante = new PersonaHumana(
        "empresa@mail.com",
        "30777888991",
        TipoDocumento.CUIT,
        "Empresa",
        "Solidaria",
        40,
        Genero.OTRO,
        "Av. Medrano 951"
    );

    RegistroDonacion registro = new RegistroDonacion(
        "4",
        "Donación de mobiliario",
        donante
    );

    Subcategoria silla = new Subcategoria("Silla");

    Bien bien = new BienConEstado(
        "Sillas de oficina",
        10,
        "UNIDAD",
        silla,
        Estado.USADO
    );

    registro.agregarBien(bien);

    List<Donacion> donaciones = registro.segmentar();

    assertEquals(1, donaciones.size());
    assertEquals(Estado.USADO, donaciones.get(0).getEstadoBien());
  }

}