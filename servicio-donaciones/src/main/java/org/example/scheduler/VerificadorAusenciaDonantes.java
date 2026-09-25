package org.example.scheduler;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioRegistroDonacion;
import org.example.dominio.donacion.RegistroDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;

public class VerificadorAusenciaDonantes {

  private final Notificador notificador;
  private final Supplier<List<Donante>> obtenerDonantes;
  private final Supplier<List<RegistroDonacion>> obtenerRegistros;
  private final EntityManagerFactory factory;

  private ScheduledExecutorService scheduler;

  // Constructor para pruebas con datos controlados.
  public VerificadorAusenciaDonantes(
      Notificador notificador,
      Supplier<List<Donante>> obtenerDonantes,
      Supplier<List<RegistroDonacion>> obtenerRegistros
  ) {
    this.notificador = Objects.requireNonNull(notificador);
    this.obtenerDonantes = Objects.requireNonNull(obtenerDonantes);
    this.obtenerRegistros = Objects.requireNonNull(obtenerRegistros);
    this.factory = null;
  }

  // Constructor para consultar los datos persistidos.
  // Quien crea la factory también se encarga de cerrarla.
  public VerificadorAusenciaDonantes(
      Notificador notificador,
      EntityManagerFactory factory
  ) {
    this.notificador = Objects.requireNonNull(notificador);
    this.factory = Objects.requireNonNull(factory);
    this.obtenerDonantes = null;
    this.obtenerRegistros = null;
  }

  public void ejecutar() {
    if (factory == null) {
      evaluar(obtenerDonantes.get(), obtenerRegistros.get());
      return;
    }

    EntityManager em = factory.createEntityManager();

    try {
      RepositorioDonantes donantes = new RepositorioDonantes(em);
      RepositorioRegistroDonacion registros =
          new RepositorioRegistroDonacion(em);

      evaluar(donantes.buscarTodos(), registros.obtenerTodos());
    } finally {
      em.close();
    }
  }

  private void evaluar(
      List<Donante> donantes,
      List<RegistroDonacion> registros
  ) {
    LocalDate hoy = LocalDate.now();

    for (Donante donante : donantes) {
      donante.notificarAusencia(registros, hoy, notificador);
    }
  }

  private void ejecutarProgramado() {
    try {
      ejecutar();
      System.out.println("Verificacion de ausencia finalizada");
    } catch (RuntimeException e) {
      // Evita que una excepción cancele las siguientes ejecuciones.
      System.err.println("Fallo la verificacion de ausencia:");
      e.printStackTrace();
    }
  }

  // Ejecuta inmediatamente y luego cada 24 horas.
  public synchronized void iniciarScheduler() {
    if (scheduler != null) {
      throw new IllegalStateException("El scheduler ya fue iniciado");
    }

    scheduler = Executors.newSingleThreadScheduledExecutor();

    scheduler.scheduleAtFixedRate(
        this::ejecutarProgramado,
        0,
        1,
        TimeUnit.DAYS
    );
  }

  public synchronized boolean detenerScheduler() {
    if (scheduler == null) {
      return true;
    }

    scheduler.shutdown();

    try {
      if (scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
        return true;
      }

      scheduler.shutdownNow();
      return scheduler.awaitTermination(30, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      Thread.currentThread().interrupt();
      return false;
    }
  }

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

    Notificador notificador = new Notificador(
        new Email(),
        new SMS(),
        new WhatsApp()
    );

    VerificadorAusenciaDonantes verificador =
        new VerificadorAusenciaDonantes(notificador, factory);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      if (verificador.detenerScheduler()) {
        factory.close();
      } else {
        // No cerrar la conexión mientras la tarea todavía la utiliza.
        System.err.println(
            "La tarea no termino dentro del tiempo de espera"
        );
      }
    }));

    try {
      verificador.iniciarScheduler();
    } catch (RuntimeException e) {
      if (verificador.detenerScheduler() && factory.isOpen()) {
        factory.close();
      }
      throw e;
    }

    System.out.println(
        "Verificador iniciado: ejecucion inmediata y luego cada 24 horas"
    );
  }
}