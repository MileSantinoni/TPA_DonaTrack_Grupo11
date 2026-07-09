package org.example.api.donaciones;

import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.api.donaciones.dto.AsignacionResponse;
import org.example.api.donaciones.dto.ConfirmarAsignacionRequest;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.Donacion;
import org.example.service.AsignacionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/asignaciones")
public class AsignacionController {

    private final RepositorioDonaciones repoDonaciones = RepositorioDonaciones.getInstance();
    private final RepositorioEntidadesBeneficiarias repoEntidades =
            RepositorioEntidadesBeneficiarias.getInstance();

    private final AsignacionService asignacionService;

    public AsignacionController(AsignacionService asignacionService) {
        this.asignacionService = asignacionService;
    }

    // Ejecuta los algoritmos a demanda para una donación y devuelve el ranking.
    @PostMapping("/ejecutar/{idDonacion}")
    public ResponseEntity<List<EntidadBeneficiaria>> ejecutarAlgoritmos(@PathVariable String idDonacion) {
        Optional<Donacion> donacionOpt = repoDonaciones.buscarPorId(idDonacion);

        if (donacionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<EntidadBeneficiaria> ranking = asignacionService.ejecutarAlgoritmos(donacionOpt.get());
        return ResponseEntity.ok(ranking);
    }

    // Devuelve el ranking ya generado para una donación.
    @GetMapping("/ranking/{idDonacion}")
    public ResponseEntity<List<EntidadBeneficiaria>> obtenerRanking(@PathVariable String idDonacion) {
        Optional<Donacion> donacionOpt = repoDonaciones.buscarPorId(idDonacion);

        if (donacionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<EntidadBeneficiaria> ranking = asignacionService.obtenerRanking(donacionOpt.get());
        return ResponseEntity.ok(ranking);
    }

    // Confirma la entidad beneficiaria final para una donación.
    @PostMapping("/confirmar")
    public ResponseEntity<?> confirmarAsignacion(@RequestBody ConfirmarAsignacionRequest request) {
        Optional<Donacion> donacionOpt = repoDonaciones.buscarPorId(request.getIdDonacion());
        Optional<EntidadBeneficiaria> entidadOpt = repoEntidades.buscarPorId(request.getIdEntidad());

        if (donacionOpt.isEmpty() || entidadOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            AsignacionDonacion asignacion =
                    asignacionService.confirmarAsignacion(donacionOpt.get(), entidadOpt.get());

            AsignacionResponse response = new AsignacionResponse(
                    asignacion.getId(),
                    asignacion.getDonacion().getId(),
                    asignacion.getEntidad().getId(),
                    asignacion.getEntidad().getRazonSocial(),
                    asignacion.getDonacion().getEstadoActual().name()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}