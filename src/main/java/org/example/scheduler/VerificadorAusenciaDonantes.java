package org.example.scheduler;

import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;
import org.example.service.NotificacionesService;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class VerificadorAusenciaDonantes {

  private final NotificacionesService notificacionesService;

  public VerificadorAusenciaDonantes(NotificacionesService notificacionesService) {
    this.notificacionesService = notificacionesService;
  }

  public void ejecutar() {
    notificacionesService.verificarAusenciaDonantes();
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

    NotificacionesService notificacionesService = new NotificacionesService(notificador);

    VerificadorAusenciaDonantes verificador = new VerificadorAusenciaDonantes(notificacionesService);
    verificador.iniciarScheduler();

    System.out.println("Verificador de ausencia de donantes iniciado...");
  }
}