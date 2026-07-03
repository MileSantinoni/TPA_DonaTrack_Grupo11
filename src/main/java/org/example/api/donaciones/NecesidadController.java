package org.example.api.donaciones;

import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.api.donaciones.dto.NecesidadExtraordinariaRequest;
import org.example.api.donaciones.dto.NecesidadRecurrenteRequest;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Necesidad;
import org.example.dominio.beneficiario.NecesidadExtraordinaria;
import org.example.dominio.beneficiario.NecesidadRecurrente;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/entidades/{idEntidad}/necesidades")
public class NecesidadController {

  private final RepositorioEntidadesBeneficiarias repositorio = RepositorioEntidadesBeneficiarias.getInstance();

  @GetMapping
  public ResponseEntity<List<Necesidad>> obtenerNecesidades(@PathVariable String idEntidad) {
    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(idEntidad);

    if (entidadOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok(entidadOpt.get().getNecesidades());
  }

  @PostMapping("/recurrentes")
  public ResponseEntity<String> registrarRecurrente(
      @PathVariable String idEntidad,
      @RequestBody NecesidadRecurrenteRequest request) {

    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(idEntidad);
    if (entidadOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    EntidadBeneficiaria entidad = entidadOpt.get();

    // mock temporal
    Subcategoria subcategoriaMock = new Subcategoria(request.getIdSubcategoria(), "Mock", TipoAtributo.NO_PERECEDERO);


    NecesidadRecurrente nuevaNecesidad = new NecesidadRecurrente(
        request.getDescripcion(),
        request.getCantidadObjetivo(),
        subcategoriaMock,
        request.getFechaInicioPeriodo(),
        request.getPeriodicidad()
    );

    entidad.registrarNecesidad(nuevaNecesidad);
    return ResponseEntity.status(HttpStatus.CREATED).body("Necesidad recurrente registrada exitosamente.");
  }

  @PostMapping("/extraordinarias")
  public ResponseEntity<String> registrarExtraordinaria(
      @PathVariable String idEntidad,
      @RequestBody NecesidadExtraordinariaRequest request) {

    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(idEntidad);
    if (entidadOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    EntidadBeneficiaria entidad = entidadOpt.get();

    Subcategoria subcategoriaMock = new Subcategoria(request.getIdSubcategoria(), "Mock", TipoAtributo.NO_PERECEDERO);


    NecesidadExtraordinaria nuevaNecesidad = new NecesidadExtraordinaria(
        request.getDescripcion(),
        request.getCantidadObjetivo(),
        subcategoriaMock,
        request.getMotivo()
    );

    entidad.registrarNecesidad(nuevaNecesidad);
    return ResponseEntity.status(HttpStatus.CREATED).body("Necesidad extraordinaria registrada exitosamente.");
  }

  @DeleteMapping("/{idNecesidad}")
  public ResponseEntity<String> eliminarNecesidad(
      @PathVariable String idEntidad,
      @PathVariable String idNecesidad) {

    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(idEntidad);
    if (entidadOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    EntidadBeneficiaria entidad = entidadOpt.get();

    boolean removida = entidad.getNecesidades().removeIf(n -> n.getId().equals(idNecesidad));

    if (removida) {
      return ResponseEntity.ok("Necesidad eliminada.");
    } else {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Necesidad no encontrada en esta entidad.");
    }
  }
}
