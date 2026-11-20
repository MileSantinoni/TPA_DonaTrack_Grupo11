package org.example.scheduler;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.persistence.*;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.notificacion.*;

public class ProcesadorNotificacionesPendientes {

  private final EntityManagerFactory factory;
  private final Notificador notificador;

  public ProcesadorNotificacionesPendientes(
      EntityManagerFactory factory,
      Notificador notificador
  ) {
    this.factory = Objects.requireNonNull(factory);
    this.notificador = Objects.requireNonNull(notificador);
  }

  public void ejecutar(int limite) {
    List<String> ids;

    EntityManager consulta = factory.createEntityManager();

    try {
      ids = new RepositorioAsignacionesDonacion(consulta)
          .buscarIdsNotificacionesPendientes(limite);
    } finally {
      consulta.close();
    }

    for (String id : ids) {
      EntityManager em = factory.createEntityManager();

      try {
        new RepositorioAsignacionesDonacion(em)
            .procesarNotificacionPendiente(id, notificador);
      } catch (RuntimeException e) {
        // Un error de persistencia no impide intentar los demás mensajes.
        System.err.println(
            "No se pudo procesar la notificacion " + id
        );
        e.printStackTrace();
      } finally {
        em.close();
      }
    }
  }

  public static void main(String[] args) {
    System.out.print("ID de la notificación que querés enviar: ");
    String id = new java.util.Scanner(System.in).nextLine().trim();

    if (id.isBlank()) {
      throw new IllegalArgumentException(
          "Ingresá el ID de una notificación"
      );
    }

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
        NotificacionPendiente pendiente =
            em.find(NotificacionPendiente.class, id);

        if (pendiente == null) {
          System.out.println(
              "No existe una notificación con ese ID en esta base."
          );
          return;
        }

        if (!pendiente.requiereEnvio()) {
          System.out.println(
              "No requiere envío. Estado: " + pendiente.getEstado()
          );
          return;
        }

        Notificador notificador =
            new Notificador(new Email(), new SMS(), new WhatsApp());

        boolean procesada =
            new RepositorioAsignacionesDonacion(em)
                .procesarNotificacionPendiente(id, notificador);

        System.out.println("Intento procesado: " + procesada);
        System.out.println("Estado: " + pendiente.getEstado());

      } finally {
        em.close();
      }

    } finally {
      factory.close();
    }
  }
}