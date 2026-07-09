package org.example.api.donaciones;

import org.example.Repositorios.RepositorioDonaciones;
import org.example.Repositorios.RepositorioDonantes;
import org.example.api.donaciones.dto.CambioEstadoRequest;
import org.example.api.donaciones.dto.DonacionRequest;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donante.Donante;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/donaciones")
public class DonacionController {

  private final RepositorioDonaciones repositorio = RepositorioDonaciones.getInstance();
  private final RepositorioDonantes repositorioDonantes = RepositorioDonantes.getInstance();


  @GetMapping
  public ResponseEntity<List<Donacion>> obtenerTodas() {
    return ResponseEntity.ok(repositorio.buscarTodas());
  }


  @GetMapping("/{id}")
  public ResponseEntity<Donacion> obtenerPorId(@PathVariable String id) {
    Optional<Donacion> donacion = repositorio.buscarPorId(id);
    return donacion.map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }


  @PostMapping
  public ResponseEntity<String> crearDonacion(@RequestBody DonacionRequest request) {
    //simulacionnnnnnnnn
    Subcategoria subcategoriaMock = new Subcategoria(request.getIdSubcategoria(), "Mock", TipoAtributo.NO_PERECEDERO);

    Optional<Donante> donanteOpt = repositorioDonantes.buscarPorId(request.getIdDonante());

    if (donanteOpt.isEmpty()) {
      return ResponseEntity
          .status(HttpStatus.NOT_FOUND)
          .body("No existe el donante: " + request.getIdDonante());
    }


    Donacion nuevaDonacion = new Donacion(
        request.getDescripcionGeneral(),
        request.getCantidad(),
        request.getUnidadMedida(),
        subcategoriaMock,
        request.getFechaVencimiento(),
        request.getEstadoBien(),
        donanteOpt.get()
    );

    repositorio.agregar(nuevaDonacion);
    return ResponseEntity.status(HttpStatus.CREATED).body("Donación creada exitosamente.");
  }


  @DeleteMapping("/{id}")
  public ResponseEntity<String> eliminarDonacion(@PathVariable String id) {
    Optional<Donacion> donacionOpt = repositorio.buscarPorId(id);
    if (donacionOpt.isPresent()) {
      repositorio.eliminar(donacionOpt.get());
      return ResponseEntity.ok("Donación eliminada exitosamente.");
    }
    return ResponseEntity.notFound().build();
  }


  @PatchMapping("/{id}/estado")
  public ResponseEntity<String> cambiarEstadoDonacion(
      @PathVariable String id,
      @RequestBody CambioEstadoRequest request) {

    Optional<Donacion> donacionOpt = repositorio.buscarPorId(id);
    if (donacionOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Donacion donacion = donacionOpt.get();

    donacion.cambiarEstado(request.getNuevoEstado(), request.getJustificativo());

    return ResponseEntity.ok("Estado de la donación actualizado a: " + request.getNuevoEstado());
  }
}