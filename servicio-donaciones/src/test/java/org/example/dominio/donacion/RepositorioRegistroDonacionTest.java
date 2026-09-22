package org.example.dominio.donacion;
import org.example.Repositorios.RepositorioRegistroDonacion;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.TipoDocumento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RepositorioRegistroDonacionTest {

  private RepositorioRegistroDonacion repositorio;
  private PersonaHumana donantePrueba;

  @BeforeEach
  public void setUp() {
    // 1. Obtenemos la instancia única del Singleton
    repositorio = RepositorioRegistroDonacion.getInstance();

    // 2. Limpiamos el repositorio antes de cada test para que no se mezclen los datos
    repositorio.limpiarRepositorio();

    // 3. Creamos un donante de prueba con tu constructor exacto
    donantePrueba = new PersonaHumana(
        "juan.perez@mail.com", // mail
        "12345678",            // numeroDocumento
        TipoDocumento.DNI,     // tipoDeDocumento
        "Juan",                // nombre
        "Pérez",               // apellido
        35,                    // edad
        Genero.MASCULINO,      // genero
        "Calle Falsa 123"      // direccion
    );
  }

  @Test
  public void testInstanciaSingletonEsUnica() {
    // Verificamos que si pedimos la instancia dos veces, el sistema nos devuelve
    // exactamente el mismo objeto en memoria (cumpliendo el patrón Singleton)
    RepositorioRegistroDonacion otraReferencia = RepositorioRegistroDonacion.getInstance();
    assertSame(repositorio, otraReferencia, "Ambas referencias deben apuntar al mismo Repositorio");
  }

  @Test
  public void testAgregarRegistroDonacion() {
    // Creamos un registro de donación con los atributos definidos en el diagrama
    RegistroDonacion nuevoRegistro = new RegistroDonacion(
        "REG-001",
        "Donación de ropa de abrigo para el invierno",
        donantePrueba
//        new Date()
    );

    // Lo agregamos a nuestro repositorio
    repositorio.agregarRegistro(nuevoRegistro);

    // Verificamos que la lista del repositorio ahora tenga 1 elemento
    assertEquals(1, repositorio.obtenerTodos().size(), "El repositorio debería tener exactamente 1 registro guardado");
  }

  @Test
  public void testBuscarRegistroPorIdExistente() {
    // Preparamos dos registros distintos
    RegistroDonacion registro1 = new RegistroDonacion("REG-001", "Donación de fideos", donantePrueba);
    RegistroDonacion registro2 = new RegistroDonacion("REG-002", "Mobiliario de oficina", donantePrueba);

    repositorio.agregarRegistro(registro1);
    repositorio.agregarRegistro(registro2);

    // Buscamos específicamente el segundo
    RegistroDonacion registroEncontrado = repositorio.buscarPorId("REG-002");

    // Verificamos que no sea nulo y que sus datos coincidan
    assertNotNull(registroEncontrado, "El repositorio debería encontrar el registro REG-002");
    assertEquals("REG-002", registroEncontrado.getId(), "El ID debe coincidir");
  }

  @Test
  public void testBuscarRegistroPorIdInexistente() {
    // Si intentamos buscar un ID que no fue guardado, el metodo debe devolver null
    RegistroDonacion registroEncontrado = repositorio.buscarPorId("REG-999");
    assertNull(registroEncontrado, "Al buscar un ID inexistente, debe retornar null");
  }
}
