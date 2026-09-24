package org.example;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.Map;
import org.example.Repositorios.RepositorioCamiones;
import org.example.dominio.logistica.Camion;

public class PruebaConexion {

  public static void main(String[] args) {
    String password = System.getenv("LOGISTICA_DB_PASSWORD");

    if (password == null || password.isBlank()) {
      throw new IllegalStateException(
          "Falta configurar LOGISTICA_DB_PASSWORD"
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
        Object base = em.createNativeQuery(
            "SELECT current_database()"
        ).getSingleResult();

        System.out.println("Conectado a: " + base);
        RepositorioCamiones repositorio = new RepositorioCamiones(em);

        String patente = "TEST001";

        if (repositorio.buscarPorPatente(patente).isEmpty()) {
          Camion camion = new Camion(patente, 20, 3, 1500);
          repositorio.agregar(camion);

          System.out.println("Camion creado y guardado.");
        } else {
          System.out.println("El camion ya estaba guardado.");
        }
      } finally {
        em.close();
      }
    } finally {
      factory.close();
    }
  }
}