package org.example.api.logistica;

import org.example.api.logistica.dto.RutaRequest;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.Entrega;
import org.example.dominio.logistica.Ruta;
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

  private final MonitoreoService servicio;

  public RutaController(MonitoreoService servicio) {
    this.servicio = servicio;
  }

  // Registra una ruta para poder monitorearla
  @PostMapping
  public ResponseEntity<String> registrarRuta(@RequestBody RutaRequest request) {
    Camion camion = servicio.buscarCamion(request.getPatente());
    if (camion == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No existe el camion: " + request.getPatente());
    }

    Ruta ruta = new Ruta(camion);
    for (RutaRequest.EntregaRequest entregaRequest : request.getEntregas()) {
      EntidadBeneficiaria destino = new EntidadBeneficiaria(
          entregaRequest.getRazonSocial(),
          entregaRequest.getDireccion(),
          entregaRequest.getTelefono());
      Entrega entrega = new Entrega(destino, entregaRequest.getOrden());
      ruta.agregarEntrega(entrega);
    }

    servicio.registrarRuta(ruta);
    return ResponseEntity.status(HttpStatus.CREATED).body("Ruta registrada para el camion " + camion.getPatente());
  }

  // El chofer da inicio a su ruta (las entregas pasan a En Traslado)
  @PostMapping("/{patente}/iniciar")
  public ResponseEntity<String> iniciarRuta(@PathVariable String patente) {
    boolean iniciada = servicio.iniciarRuta(patente);
    if (iniciada) {
      return ResponseEntity.ok("Ruta iniciada");
    }
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No hay ruta para el camion: " + patente);
  }
}
