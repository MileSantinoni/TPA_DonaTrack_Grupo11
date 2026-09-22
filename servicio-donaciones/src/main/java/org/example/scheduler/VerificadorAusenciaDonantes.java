package org.example.scheduler;

import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;
import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioRegistroDonacion;
import java.time.LocalDate;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VerificadorAusenciaDonantes {

  private final Notificador notificador;

  public VerificadorAusenciaDonantes(Notificador notificador) {
    this.notificador = notificador;
  }

  public void ejecutar() {
    var registros = RepositorioRegistroDonacion.getInstance().obtenerTodos();
    LocalDate hoy = LocalDate.now();
    RepositorioDonantes.getInstance().buscarTodos()
        .forEach(donante -> donante.notificarAusencia(registros, hoy, notificador));
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
        new Email(null),
        new SMS(),
        new WhatsApp()
    );


    VerificadorAusenciaDonantes verificador = new VerificadorAusenciaDonantes(notificador);
    verificador.iniciarScheduler();

    System.out.println("Verificador de ausencia de donantes iniciado...");
  }
}
