package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.example.api.donaciones.AsignacionController;
import org.example.api.donaciones.DonacionController;
import org.example.api.donaciones.DonanteController;
import org.example.api.donaciones.EntidadBeneficiariaController;
import org.example.api.donaciones.IntegracionLogisticaController;
import org.example.api.donaciones.NecesidadController;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;

public class DonacionesApplication {
  public static Javalin crearApp() {
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    Javalin app = Javalin.create(config -> config.jsonMapper(new JavalinJackson(mapper)));

    DonanteController donantes = new DonanteController(RepositorioDonantes.getInstance());
    DonacionController donaciones = new DonacionController(
        RepositorioDonaciones.getInstance(), RepositorioDonantes.getInstance());
    EntidadBeneficiariaController entidades = new EntidadBeneficiariaController(
        RepositorioEntidadesBeneficiarias.getInstance());
    NecesidadController necesidades = new NecesidadController(
        RepositorioEntidadesBeneficiarias.getInstance());
    AsignacionController asignaciones = new AsignacionController();
    IntegracionLogisticaController integracion = new IntegracionLogisticaController();

    app.get("/health", ctx -> ctx.result("OK"));

    app.get("/donantes", donantes::obtenerTodos);
    app.get("/donantes/{id}", donantes::obtenerPorId);
    app.post("/donantes/humanas", donantes::crearPersonaHumana);
    app.put("/donantes/humanas/{id}", donantes::actualizarPersonaHumana);
    app.post("/donantes/juridicas", donantes::crearPersonaJuridica);
    app.put("/donantes/juridicas/{id}", donantes::actualizarPersonaJuridica);
    app.delete("/donantes/{id}", donantes::eliminarDonante);

    app.get("/donaciones", donaciones::obtenerTodas);
    app.get("/donaciones/{id}", donaciones::obtenerPorId);
    app.post("/donaciones", donaciones::crearDonacion);
    app.patch("/donaciones/{id}/estado", donaciones::cambiarEstadoDonacion);
    app.delete("/donaciones/{id}", donaciones::eliminarDonacion);

    app.get("/entidades", entidades::obtenerTodas);
    app.get("/entidades/{id}", entidades::obtenerPorId);
    app.post("/entidades", entidades::crearEntidad);
    app.put("/entidades/{id}", entidades::actualizarEntidad);
    app.delete("/entidades/{id}", entidades::eliminarEntidad);
    app.get("/entidades/{idEntidad}/necesidades", necesidades::obtenerNecesidades);
    app.post("/entidades/{idEntidad}/necesidades/recurrentes", necesidades::registrarRecurrente);
    app.post("/entidades/{idEntidad}/necesidades/extraordinarias", necesidades::registrarExtraordinaria);
    app.delete("/entidades/{idEntidad}/necesidades/{idNecesidad}", necesidades::eliminarNecesidad);

    app.post("/asignaciones/ejecutar/{idDonacion}", asignaciones::ejecutarAlgoritmos);
    app.get("/asignaciones/ranking/{idDonacion}", asignaciones::obtenerRanking);
    app.post("/asignaciones/confirmar", asignaciones::confirmarAsignacion);
    app.post("/asignaciones/rechazar", asignaciones::rechazarAsignacion);

    app.get("/interno/asignaciones", integracion::listarAsignaciones);
    app.get("/interno/asignaciones/{id}", integracion::buscarAsignacion);
    app.get("/interno/donaciones/{id}/existe", integracion::existeDonacion);
    return app;
  }

  public static void main(String[] args) {
    crearApp().start(8080);
  }
}
