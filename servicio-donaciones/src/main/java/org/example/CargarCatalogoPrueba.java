package org.example;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.Repositorios.RepositorioCatalogo;
import org.example.dominio.catalogo.Categoria;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;

public class CargarCatalogoPrueba {

  public static void main(String[] args) {
    String password = System.getenv("DONACIONES_DB_PASSWORD");

    if (password == null || password.isBlank()) {
      throw new IllegalStateException(
          "Falta configurar DONACIONES_DB_PASSWORD"
      );
    }

    EntityManagerFactory factory =
        Persistence.createEntityManagerFactory(
            "simple-persistence-unit",
            Map.of("javax.persistence.jdbc.password", password)
        );

    try {
      EntityManager em = factory.createEntityManager();

      try {
        RepositorioCatalogo catalogo = new RepositorioCatalogo(em);

        if (catalogo.buscarSubcategoria("ALIMENTOS-SECOS").isPresent()) {
          System.out.println("La subcategoria ya existe");
          return;
        }

        Categoria categoria = new Categoria("Alimentos de prueba");

        categoria.agregarSubcategoria(new Subcategoria(
            "ALIMENTOS-SECOS",
            "Alimentos secos",
            TipoAtributo.NO_PERECEDERO
        ));

        catalogo.agregarCategoria(categoria);

        System.out.println("Subcategoria creada: ALIMENTOS-SECOS");
      } finally {
        em.close();
      }
    } finally {
      factory.close();
    }
  }
}