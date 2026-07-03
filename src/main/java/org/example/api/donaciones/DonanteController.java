package org.example.api.donaciones;

import org.example.Repositorios.RepositorioDonantes;
import org.example.api.donaciones.dto.PersonaHumanaRequest;
import org.example.api.donaciones.dto.PersonaJuridicaRequest;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.PersonaJuridica;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/donantes")
public class DonanteController {

  private final RepositorioDonantes repositorio = RepositorioDonantes.getInstance();

  @GetMapping
  public ResponseEntity<List<Donante>> obtenerTodos() {
    return ResponseEntity.ok(repositorio.buscarTodos());
  }

  @GetMapping("/{id}")
  public ResponseEntity<Donante> obtenerPorId(@PathVariable String id) {
    Optional<Donante> donante = repositorio.buscarPorId(id);
    return donante.map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @PostMapping("/humanas")
  public ResponseEntity<String> crearPersonaHumana(@RequestBody PersonaHumanaRequest request) {
    if (repositorio.existeMail(request.getMail())) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El mail ya está registrado.");
    }

    PersonaHumana nuevoDonante = new PersonaHumana(
        request.getMail(),
        request.getNumeroDocumento(),
        request.getTipoDeDocumento(),
        request.getNombre(),
        request.getApellido(),
        request.getEdad(),
        request.getGenero(),
        request.getDireccion()
    );

    // atención puede haber problemas con el id en esta parte

    repositorio.agregar(nuevoDonante);
    return ResponseEntity.status(HttpStatus.CREATED).body("Donante creado exitosamente.");
  }


  @DeleteMapping("/{id}")
  public ResponseEntity<String> eliminarDonante(@PathVariable String id) {
    Optional<Donante> donanteOpt = repositorio.buscarPorId(id);
    if (donanteOpt.isPresent()) {
      repositorio.eliminar(donanteOpt.get());
      return ResponseEntity.ok("Donante eliminado exitosamente.");
    }
    return ResponseEntity.notFound().build();
  }


  // actualiza persona humana
  @PutMapping("/humanas/{id}")
  public ResponseEntity<String> actualizarPersonaHumana(@PathVariable String id, @RequestBody PersonaHumanaRequest request) {
    Optional<Donante> donanteOpt = repositorio.buscarPorId(id);

    if (donanteOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Donante donanteExistente = donanteOpt.get();


    if (!(donanteExistente instanceof PersonaHumana)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El ID no corresponde a una Persona Humana.");
    }

    PersonaHumana persona = (PersonaHumana) donanteExistente;


    persona.actualizarDatos(request.getMail(), request.getNumeroDocumento(), request.getTipoDeDocumento());


    persona.setNombre(request.getNombre());
    persona.setApellido(request.getApellido());
    persona.setEdad(request.getEdad());
    persona.setGenero(request.getGenero());
    persona.setDireccion(request.getDireccion());

    return ResponseEntity.ok("Persona Humana actualizada exitosamente.");
  }






  // de la persona jurídica
  @PostMapping("/juridicas")
  public ResponseEntity<String> crearPersonaJuridica(@RequestBody PersonaJuridicaRequest request) {
    if (repositorio.existeMail(request.getMail())) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El mail ya está registrado.");
    }


    PersonaJuridica nuevoDonante = new PersonaJuridica(
        request.getMail(),
        request.getNumeroDocumento(),
        request.getTipoDeDocumento(),
        request.getRazonSocial(),
        request.getTipo(),
        request.getRubro()
    );

    repositorio.agregar(nuevoDonante);
    return ResponseEntity.status(HttpStatus.CREATED).body("Persona Jurídica creada exitosamente.");
  }



  // actualiza persona juridica
  @PutMapping("/juridicas/{id}")
  public ResponseEntity<String> actualizarPersonaJuridica(@PathVariable String id, @RequestBody PersonaJuridicaRequest request) {
    Optional<Donante> donanteOpt = repositorio.buscarPorId(id);

    if (donanteOpt.isEmpty()) {
      return ResponseEntity.notFound().build();
    }

    Donante donanteExistente = donanteOpt.get();

    if (!(donanteExistente instanceof PersonaJuridica)) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("El ID no corresponde a una Persona Jurídica.");
    }

    PersonaJuridica persona = (PersonaJuridica) donanteExistente;

    persona.actualizarDatos(request.getMail(), request.getNumeroDocumento(), request.getTipoDeDocumento());


    persona.setRazonSocial(request.getRazonSocial());
    persona.setTipo(request.getTipo());
    persona.setRubro(request.getRubro());

    return ResponseEntity.ok("Persona Jurídica actualizada exitosamente.");
  }


}
