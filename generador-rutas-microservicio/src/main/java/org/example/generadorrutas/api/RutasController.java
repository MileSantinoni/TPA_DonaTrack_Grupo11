package org.example.generadorrutas.api;

import java.util.List;
import org.example.generadorrutas.dto.GenerarRutasRequest;
import org.example.generadorrutas.dto.RutaGeneradaResponse;
import org.example.generadorrutas.dominio.GeneradorDeRutas;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rutas")
public class RutasController {

  private final GeneradorDeRutas generadorDeRutas;

  public RutasController(GeneradorDeRutas generadorDeRutas) {
    this.generadorDeRutas = generadorDeRutas;
  }

  @PostMapping("/generar")
  public ResponseEntity<List<RutaGeneradaResponse>> generarRutas(
      @RequestBody GenerarRutasRequest request
  ) {
    return ResponseEntity.ok(generadorDeRutas.generar(request));
  }
}
