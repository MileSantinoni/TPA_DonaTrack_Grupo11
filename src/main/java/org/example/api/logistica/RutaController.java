package org.example.api.logistica;

import org.example.api.logistica.dto.RutaRequest;
import org.example.service.RutaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rutas")
public class RutaController {

  private final RutaService rutaService;

  public RutaController(RutaService rutaService) {
    this.rutaService = rutaService;
  }

  @PostMapping
  public ResponseEntity<String> registrarRuta(@RequestBody RutaRequest request) {
    boolean registrada = rutaService.registrarRuta(request);

    if (!registrada) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body("No se pudo registrar la ruta");
    }

    return ResponseEntity.status(HttpStatus.CREATED)
        .body("Ruta registrada");
  }

  @PostMapping("/{patente}/iniciar")
  public ResponseEntity<String> iniciarRuta(@PathVariable String patente) {
    boolean iniciada = rutaService.iniciarRuta(patente);

    if (iniciada) {
      return ResponseEntity.ok("Ruta iniciada");
    }

    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body("No hay ruta para el camion: " + patente);
  }
}
