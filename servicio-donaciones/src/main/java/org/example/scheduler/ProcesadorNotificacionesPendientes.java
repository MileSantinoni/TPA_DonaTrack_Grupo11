package org.example.scheduler;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.notificacion.Notificador;

public class ProcesadorNotificacionesPendientes implements AutoCloseable {

  private final EntityManagerFactory factory;
  private final Notificador notificador;

  private final ScheduledExecutorService executor =
      Executors.newSingleThreadScheduledExecutor(tarea -> {
        Thread hilo = new Thread(tarea, "donatrack-notificaciones");
        hilo.setDaemon(true);
        return hilo;
      });

  private boolean iniciado;
  private boolean cerrado;

  public ProcesadorNotificacionesPendientes(
      EntityManagerFactory factory,
      Notificador notificador
  ) {
    this.factory = Objects.requireNonNull(factory);
    this.notificador = Objects.requireNonNull(notificador);
  }

  public synchronized void iniciar() {
    if (iniciado || cerrado) {
      return;
    }

    iniciado = true;

    executor.scheduleWithFixedDelay(() -> {
      try {
        ejecutar(1);
      } catch (RuntimeException e) {
        System.err.println(
            "No se pudo revisar la cola de notificaciones"
        );
        e.printStackTrace();
      }
    }, 0, 4, TimeUnit.SECONDS);
  }

  public synchronized void ejecutar(int limite) {
    if (cerrado) {
      return;
    }

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
        System.err.println(
            "No se pudo procesar la notificación " + id
        );
        e.printStackTrace();
      } finally {
        em.close();
      }
    }
  }

  @Override
  public synchronized void close() {
    // Espera a que termine ejecutar antes de cerrar el procesador.
    cerrado = true;
    executor.shutdownNow();
  }
}