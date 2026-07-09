package org.example.service;

import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.Repositorios.RepositorioResultadosAlgoritmos;
import org.example.dominio.algoritmo.AlgoritmoMadre;
import org.example.dominio.algoritmo.ResultadoEjecucionAlgoritmos;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Necesidad;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AsignacionService {

    private final RepositorioEntidadesBeneficiarias repoEntidades =
            RepositorioEntidadesBeneficiarias.getInstance();
    private final RepositorioResultadosAlgoritmos repoResultados =
            RepositorioResultadosAlgoritmos.getInstance();
    private final RepositorioAsignacionesDonacion repoAsignaciones =
            RepositorioAsignacionesDonacion.getInstance();

    private final AlgoritmoMadre algoritmoMadre = new AlgoritmoMadre();
    private final NotificacionesService notificacionesService;

    public AsignacionService(NotificacionesService notificacionesService) {
        this.notificacionesService = notificacionesService;
    }

    // Ejecución a demanda de los algoritmos para una donación puntual.
    public List<EntidadBeneficiaria> ejecutarAlgoritmos(Donacion donacion) {
        List<EntidadBeneficiaria> entidades = repoEntidades.buscarTodas();
        List<EntidadBeneficiaria> propuestas = algoritmoMadre.proponer(donacion, entidades);

        repoResultados.eliminarPorDonacion(donacion.getId());
        repoResultados.agregar(new ResultadoEjecucionAlgoritmos(donacion, propuestas));

        return propuestas;
    }

    // Ranking ya generado para una donación (vacío si todavía no se ejecutó).
    public List<EntidadBeneficiaria> obtenerRanking(Donacion donacion) {
        List<ResultadoEjecucionAlgoritmos> resultados =
                repoResultados.buscarPorDonacion(donacion.getId());

        if (resultados.isEmpty()) {
            return new ArrayList<>();
        }

        ResultadoEjecucionAlgoritmos ultimo = resultados.get(0);
        for (ResultadoEjecucionAlgoritmos resultado : resultados) {
            if (resultado.getFechaYHoraEjecucion().isAfter(ultimo.getFechaYHoraEjecucion())) {
                ultimo = resultado;
            }
        }

        return ultimo.getEntidadesPropuestas();
    }

    // Selección de la entidad beneficiaria final: crea la asignación, actualiza
    // el estado de la donación y dispara las notificaciones al beneficiario y al donante.
    public AsignacionDonacion confirmarAsignacion(Donacion donacion, EntidadBeneficiaria entidad) {
        Necesidad necesidad = primeraNecesidad(entidad);

        AsignacionDonacion asignacion =
                new AsignacionDonacion(donacion, entidad, necesidad, LocalDate.now());

        repoAsignaciones.agregar(asignacion);

        donacion.cambiarEstado(
                EstadoDonacion.ASIGNACION_REALIZADA,
                "Asignada a la entidad beneficiaria " + entidad.getRazonSocial()
        );

        notificacionesService.notificarDonacionAsignadaBeneficiario(asignacion);
        notificacionesService.notificarDonacionAsignadaDonante(asignacion);

        return asignacion;
    }

    private Necesidad primeraNecesidad(EntidadBeneficiaria entidad) {
        for (Necesidad necesidad : entidad.getNecesidades()) {
            return necesidad;
        }

        return null;
    }
}