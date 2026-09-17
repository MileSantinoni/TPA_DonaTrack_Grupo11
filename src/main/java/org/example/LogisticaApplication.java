package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.example.Routes.RoutesLogistica;
import org.example.Routes.RoutesDonaciones;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.api.donaciones.AsignacionController;
import org.example.api.logistica.CamionController;
import org.example.api.logistica.RutaController;

import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;
import org.example.dominio.logistica.MonitorCamiones;
import org.example.api.donaciones.DonanteController;
import org.example.api.donaciones.DonacionController;
import org.example.api.donaciones.EntidadBeneficiariaController;
import org.example.api.donaciones.NecesidadController;

public class LogisticaApplication {

  public static Javalin crearApp() {
    return crearApp(new Notificador(new Email(null), new SMS(), new WhatsApp()));
  }

  public static Javalin crearApp(Notificador notificador) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    Javalin app = Javalin.create(config -> {
      config.jsonMapper(new JavalinJackson(objectMapper));
    });

    RepositorioDonantes repoDonantes = RepositorioDonantes.getInstance();
    RepositorioDonaciones repoDonaciones = RepositorioDonaciones.getInstance();
    RepositorioEntidadesBeneficiarias repoEntidades = RepositorioEntidadesBeneficiarias.getInstance();

    CamionController camionController = new CamionController();
    RutaController rutaController = new RutaController(MonitorCamiones.getInstance(), notificador);
    AsignacionController asignacionController = new AsignacionController();

    DonanteController donanteController = new DonanteController(repoDonantes);
    DonacionController donacionController = new DonacionController(repoDonaciones, repoDonantes);
    EntidadBeneficiariaController entidadController = new EntidadBeneficiariaController(repoEntidades);
    NecesidadController necesidadController = new NecesidadController(repoEntidades);

    RoutesLogistica.registrar(app, camionController, rutaController);
    RoutesDonaciones.registrar(app, asignacionController, donanteController,
        donacionController, entidadController, necesidadController);

    return app;
  }

  public static void main(String[] args) {
    crearApp().start(8080);
  }
}
