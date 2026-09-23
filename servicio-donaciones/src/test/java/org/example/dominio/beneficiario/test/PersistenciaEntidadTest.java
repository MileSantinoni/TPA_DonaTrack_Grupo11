package org.example.dominio.beneficiario.test;

import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PersistenciaEntidadTest {

  private RepositorioEntidadesBeneficiarias repositorio;

  @BeforeEach
  public void setUp() {
    repositorio = RepositorioEntidadesBeneficiarias.getInstance();
    repositorio.limpiar(); // Dejamos la tabla vacía antes de probar
  }

  @Test
  public void testPersistirYRecuperarEntidad() {
    // 1. Instanciamos la entidad
    EntidadBeneficiaria comedor = new EntidadBeneficiaria("Comedor La Esperanza", "Av. Libertador 100", "1122334455");

    // 2. Persistimos en PostgreSQL
    repositorio.agregar(comedor);

    // 3. Comprobamos que JPA le asignó un UUID exitosamente
    assertNotNull(comedor.getId(), "La entidad debe tener un ID luego de persistirse");

    // 4. Comprobamos que la podemos recuperar de la BD
    List<EntidadBeneficiaria> guardadas = repositorio.buscarTodos();
    assertEquals(1, guardadas.size());
    assertEquals("Comedor La Esperanza", guardadas.get(0).getRazonSocial());

    System.out.println("Éxito! Entidad persistida en BD con ID: " + comedor.getId());
  }
}