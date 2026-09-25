package org.example.scheduler;

import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;
import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioRegistroDonacion;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.example.dominio.donante.Donante;
import org.example.dominio.donacion.RegistroDonacion;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VerificadorAusenciaDonantes {

  private final Notificador notificador;
  private final Supplier<List<Donante>> obtenerDonantes;
  private final Supplier<List<RegistroDonacion>> obtenerRegistros;

  // Compatibilidad temporal con los llamados actuales.
  public VerificadorAusenciaDonantes(Notificador notificador) {
    this(
        notificador,
        () -> RepositorioDonantes.getInstance().buscarTodos(),
        () -> RepositorioRegistroDonacion.getInstance().obtenerTodos()
    );
  }

  public VerificadorAusenciaDonantes(
      Notificador notificador,
      Supplier<List<Donante>> obtenerDonantes,
      Supplier<List<RegistroDonacion>> obtenerRegistros
  ) {
    this.notificador = Objects.requireNonNull(notificador);
    this.obtenerDonantes = Objects.requireNonNull(obtenerDonantes);
    this.obtenerRegistros = Objects.requireNonNull(obtenerRegistros);
  }

  public void ejecutar() {
    List<RegistroDonacion> registros = obtenerRegistros.get();
    LocalDate hoy = LocalDate.now();

    obtenerDonantes.get().forEach(
        donante -> donante.notificarAusencia(registros, hoy, notificador)
    );
  }

  // ejecución periódica a las 9 AM
  public void iniciarScheduler() {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    scheduler.scheduleAtFixedRate(
        this::ejecutar,
        0,
        1,
        TimeUnit.DAYS
    );
  }

  public static void main(String[] args) {
    // instancia del notificador
    Notificador notificador = new Notificador(
        new Email(),
        new SMS(),
        new WhatsApp()
    );


    VerificadorAusenciaDonantes verificador = new VerificadorAusenciaDonantes(notificador);
    verificador.iniciarScheduler();

    System.out.println("Verificador de ausencia de donantes iniciado...");
  }
}
