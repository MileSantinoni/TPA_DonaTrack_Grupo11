package org.example.api.donaciones;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.Repositorios.RepositorioResultadosAlgoritmos;
import org.example.api.donaciones.dto.AsignacionResponse;
import org.example.api.donaciones.dto.ConfirmarAsignacionRequest;
import org.example.api.donaciones.dto.RechazarAsignacionRequest;
import org.example.dominio.algoritmo.AlgoritmoMadre;
import org.example.dominio.algoritmo.ResultadoEjecucionAlgoritmos;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AsignacionController {

    private final RepositorioDonaciones repoDonaciones;
    private final RepositorioEntidadesBeneficiarias repoEntidades;
    private final RepositorioResultadosAlgoritmos repoResultados;
    private final RepositorioAsignacionesDonacion repoAsignaciones;
    private final AlgoritmoMadre algoritmoMadre;
    private final Notificador notificador;

    public AsignacionController() {
        this(
            RepositorioDonaciones.getInstance(),
            RepositorioEntidadesBeneficiarias.getInstance(),
            RepositorioResultadosAlgoritmos.getInstance(),
            RepositorioAsignacionesDonacion.getInstance(),
            new AlgoritmoMadre(),
            new Notificador(new Email(null), new SMS(), new WhatsApp())
        );
    }

    public AsignacionController(RepositorioDonaciones repoDonaciones,
                                RepositorioEntidadesBeneficiarias repoEntidades,
                                RepositorioResultadosAlgoritmos repoResultados,
                                RepositorioAsignacionesDonacion repoAsignaciones,
                                AlgoritmoMadre algoritmoMadre,
                                Notificador notificador) {
        this.repoDonaciones = repoDonaciones;
        this.repoEntidades = repoEntidades;
        this.repoResultados = repoResultados;
        this.repoAsignaciones = repoAsignaciones;
        this.algoritmoMadre = algoritmoMadre;
        this.notificador = notificador;
    }

    // Ejecuta los algoritmos a demanda para una donación y devuelve el ranking.
    public void ejecutarAlgoritmos(Context ctx) {
        String idDonacion = ctx.pathParam("idDonacion");
        Optional<Donacion> donacionOpt = repoDonaciones.buscarPorId(idDonacion);

        if (donacionOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        Donacion donacion = donacionOpt.get();
        List<EntidadBeneficiaria> propuestas = algoritmoMadre.proponer(donacion, repoEntidades.buscarTodas());
        repoResultados.eliminarPorDonacion(donacion.getId());
        repoResultados.agregar(new ResultadoEjecucionAlgoritmos(donacion, propuestas));

        ctx.status(HttpStatus.OK).json(propuestas);
    }

    // Devuelve el ranking ya generado para una donación.
    public void obtenerRanking(Context ctx) {
        String idDonacion = ctx.pathParam("idDonacion");
        Optional<Donacion> donacionOpt = repoDonaciones.buscarPorId(idDonacion);

        if (donacionOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        List<ResultadoEjecucionAlgoritmos> resultados =
            repoResultados.buscarPorDonacion(donacionOpt.get().getId());

        if (resultados.isEmpty()) {
            ctx.status(HttpStatus.OK).json(new ArrayList<>());
            return;
        }

        ResultadoEjecucionAlgoritmos ultimo = resultados.stream()
            .max(Comparator.comparing(ResultadoEjecucionAlgoritmos::getFechaYHoraEjecucion))
            .orElse(resultados.get(0));

        ctx.status(HttpStatus.OK).json(ultimo.getEntidadesPropuestas());
    }

    // Confirma (acepta) la entidad beneficiaria final para una donación.
    public void confirmarAsignacion(Context ctx) {
        ConfirmarAsignacionRequest request = ctx.bodyAsClass(ConfirmarAsignacionRequest.class);
        Optional<Donacion> donacionOpt = repoDonaciones.buscarPorId(request.getIdDonacion());
        Optional<EntidadBeneficiaria> entidadOpt = repoEntidades.buscarPorId(request.getIdEntidad());

        if (donacionOpt.isEmpty() || entidadOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        Donacion donacion = donacionOpt.get();
        EntidadBeneficiaria entidad = entidadOpt.get();

        try {
            AsignacionDonacion asignacion = donacion.asignarA(entidad, notificador);
            repoAsignaciones.agregar(asignacion);


            AsignacionResponse response = new AsignacionResponse(
                asignacion.getId(),
                asignacion.getDonacion().getId(),
                asignacion.getEntidad().getId(),
                asignacion.getEntidad().getRazonSocial(),
                asignacion.getDonacion().getEstadoActual().name()
            );

            ctx.status(HttpStatus.CREATED).json(response);
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.CONFLICT).result(e.getMessage());
        }
    }

    // Rechaza la propuesta de asignación sugerida para una donación.
    public void rechazarAsignacion(Context ctx) {
        RechazarAsignacionRequest request = ctx.bodyAsClass(RechazarAsignacionRequest.class);
        Optional<Donacion> donacionOpt = repoDonaciones.buscarPorId(request.getIdDonacion());

        if (donacionOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        repoResultados.eliminarPorDonacion(donacionOpt.get().getId());
        ctx.status(HttpStatus.OK).result("Asignacion rechazada");
    }
}
