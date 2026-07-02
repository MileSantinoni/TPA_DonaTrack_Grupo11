package org.example.api.logistica;

import org.example.api.logistica.dto.AvanceResponse;
import org.example.api.logistica.dto.CamionRequest;
import org.example.api.logistica.dto.ReporteUbicacionRequest;
import org.example.api.logistica.dto.UbicacionResponse;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.ReporteUbicacion;
import org.example.dominio.logistica.UbicacionCamion;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/camiones")
public class CamionController {

  private final MonitoreoService servicio;

  public CamionController(MonitoreoService servicio) {
    this.servicio = servicio;
  }

  // Alta de un camión en la flota
  @PostMapping
  public ResponseEntity<String> registrarCamion(@RequestBody CamionRequest request) {
    Camion camion = new Camion(
        request.getPatente(),
        request.getCapacidadVolumen(),
        request.getAltura(),
        request.getCapacidadCarga());
    servicio.registrarCamion(camion);
    return ResponseEntity.status(HttpStatus.CREATED).body("Camion registrado: " + camion.getPatente());
  }

  // Contrato de integración: la app móvil reporta la ubicación del camión
  @PostMapping("/ubicacion")
  public ResponseEntity<String> recibirUbicacion(@RequestBody ReporteUbicacionRequest request) {
    LocalDateTime fecha = request.getFechaYHora();
    if (fecha == null) {
      fecha = LocalDateTime.now();
    }

    ReporteUbicacion reporte = new ReporteUbicacion(
        request.getPatente(),
        request.getLatitud(),
        request.getLongitud(),
        request.getVelocidad(),
        fecha);

    boolean procesado = servicio.recibirReporte(reporte);
    if (procesado) {
      return ResponseEntity.ok("Ubicacion registrada");
    }
    return ResponseEntity.badRequest().body("Reporte rechazado por validacion");
  }

  // Dashboard: última ubicación conocida del camión
  @GetMapping("/{patente}/ubicacion")
  public ResponseEntity<UbicacionResponse> ubicacionActual(@PathVariable String patente) {
    UbicacionCamion ubicacion = servicio.ubicacionActual(patente);
    if (ubicacion == null) {
      return ResponseEntity.notFound().build();
    }

    UbicacionResponse response = new UbicacionResponse(
        ubicacion.getLatitud(),
        ubicacion.getLongitud(),
        ubicacion.getVelocidad(),
        ubicacion.getFechaYHora());
    return ResponseEntity.ok(response);
  }

  // Dashboard: avance del camión sobre su ruta
  @GetMapping("/{patente}/avance")
  public ResponseEntity<AvanceResponse> avanceDeRuta(@PathVariable String patente) {
    double avance = servicio.avanceDeRuta(patente);
    return ResponseEntity.ok(new AvanceResponse(patente, avance));
  }
}
