package org.example.api.donaciones;

import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.api.donaciones.dto.EntidadBeneficiariaRequest;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/entidades")
public class EntidadBeneficiariaController {

  // Instanciamos tu nuevo repositorio Singleton
  private final RepositorioEntidadesBeneficiarias repositorio = RepositorioEntidadesBeneficiarias.getInstance();

  // 1. LEER TODAS (GET)
  @GetMapping
  public ResponseEntity<List<EntidadBeneficiaria>> obtenerTodas() {
    return ResponseEntity.ok(repositorio.buscarTodas());
  }

  // 2. LEER POR ID (GET)
  @GetMapping("/{id}")
  public ResponseEntity<EntidadBeneficiaria> obtenerPorId(@PathVariable String id) {
    Optional<EntidadBeneficiaria> entidad = repositorio.buscarPorId(id);
    return entidad.map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  // 3. CREAR ENTIDAD (POST)
  @PostMapping
  public ResponseEntity<String> crearEntidad(@RequestBody EntidadBeneficiariaRequest request) {
    // Asumiendo que tu constructor recibe razonSocial, direccion y telefono [2]
    EntidadBeneficiaria nuevaEntidad = new EntidadBeneficiaria(
        request.getRazonSocial(),
        request.getDireccion(),
        request.getTelefono()
    );

    repositorio.agregar(nuevaEntidad);
    return ResponseEntity.status(HttpStatus.CREATED).body("Entidad Beneficiaria creada exitosamente.");
  }

  // 4. ACTUALIZAR ENTIDAD (PUT)
  @PutMapping("/{id}")
  public ResponseEntity<String> actualizarEntidad(@PathVariable String id, @RequestBody EntidadBeneficiariaRequest request) {
    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(id);

    if (entidadOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    EntidadBeneficiaria entidad = entidadOpt.get();

    // Asumiendo que tenés estos setters en tu clase EntidadBeneficiaria
    entidad.setRazonSocial(request.getRazonSocial());
    entidad.setDireccion(request.getDireccion());
    entidad.setTelefono(request.getTelefono());

    return ResponseEntity.ok("Entidad Beneficiaria actualizada exitosamente.");
  }

  // 5. ELIMINAR ENTIDAD (DELETE)
  @DeleteMapping("/{id}")
  public ResponseEntity<String> eliminarEntidad(@PathVariable String id) {
    Optional<EntidadBeneficiaria> entidadOpt = repositorio.buscarPorId(id);

    if (entidadOpt.isPresent()) {
      repositorio.eliminar(entidadOpt.get());
      return ResponseEntity.ok("Entidad Beneficiaria eliminada exitosamente.");
    }

    return ResponseEntity.notFound().build();
  }
}