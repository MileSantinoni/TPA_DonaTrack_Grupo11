package org.example.api.donaciones;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.Repositorios.RepositorioDonantes;
import org.example.api.donaciones.dto.PersonaHumanaRequest;
import org.example.api.donaciones.dto.PersonaJuridicaRequest;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.PersonaJuridica;

import java.util.List;
import java.util.Optional;

public class DonanteController {

  private final RepositorioDonantes repositorio;

  public DonanteController(RepositorioDonantes repositorio) {
    this.repositorio = repositorio;
  }


  public void obtenerTodos(Context ctx) {
    List<Donante> donantes = repositorio.buscarTodos();
    ctx.status(HttpStatus.OK).json(donantes);
  }


  public void obtenerPorId(Context ctx) {
    String id = ctx.pathParam("id");
    Optional<Donante> donante = repositorio.buscarPorId(id);

    if (donante.isPresent()) {
      ctx.status(HttpStatus.OK).json(donante.get());
    } else {
      ctx.status(HttpStatus.NOT_FOUND);
    }
  }


  public void crearPersonaHumana(Context ctx) {
    PersonaHumanaRequest request = ctx.bodyAsClass(PersonaHumanaRequest.class);

    if (repositorio.existeMail(request.getMail())) {
      ctx.status(HttpStatus.BAD_REQUEST).result("El mail ya está registrado.");
      return;
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

    repositorio.agregar(nuevoDonante);
    ctx.status(HttpStatus.CREATED).result("Donante creado exitosamente.");
  }


  public void eliminarDonante(Context ctx) {
    String id = ctx.pathParam("id");
    Optional<Donante> donanteOpt = repositorio.buscarPorId(id);

    if (donanteOpt.isPresent()) {
      repositorio.eliminar(donanteOpt.get());
      ctx.status(HttpStatus.OK).result("Donante eliminado exitosamente.");
    } else {
      ctx.status(HttpStatus.NOT_FOUND);
    }
  }


  public void actualizarPersonaHumana(Context ctx) {
    String id = ctx.pathParam("id");
    PersonaHumanaRequest request = ctx.bodyAsClass(PersonaHumanaRequest.class);

    Optional<Donante> donanteOpt = repositorio.buscarPorId(id);

    if (donanteOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }

    Donante donanteExistente = donanteOpt.get();

    if (!(donanteExistente instanceof PersonaHumana)) {
      ctx.status(HttpStatus.BAD_REQUEST).result("El ID no corresponde a una Persona Humana.");
      return;
    }

    PersonaHumana persona = (PersonaHumana) donanteExistente;

    persona.actualizarDatos(request.getMail(), request.getNumeroDocumento(), request.getTipoDeDocumento());
    persona.setNombre(request.getNombre());
    persona.setApellido(request.getApellido());
    persona.setEdad(request.getEdad());
    persona.setGenero(request.getGenero());
    persona.setDireccion(request.getDireccion());

    ctx.status(HttpStatus.OK).result("Persona Humana actualizada exitosamente.");
  }


  public void crearPersonaJuridica(Context ctx) {
    PersonaJuridicaRequest request = ctx.bodyAsClass(PersonaJuridicaRequest.class);

    if (repositorio.existeMail(request.getMail())) {
      ctx.status(HttpStatus.BAD_REQUEST).result("El mail ya está registrado.");
      return;
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
    ctx.status(HttpStatus.CREATED).result("Persona Jurídica creada exitosamente.");
  }


  public void actualizarPersonaJuridica(Context ctx) {
    String id = ctx.pathParam("id");
    PersonaJuridicaRequest request = ctx.bodyAsClass(PersonaJuridicaRequest.class);

    Optional<Donante> donanteOpt = repositorio.buscarPorId(id);

    if (donanteOpt.isEmpty()) {
      ctx.status(HttpStatus.NOT_FOUND);
      return;
    }

    Donante donanteExistente = donanteOpt.get();

    if (!(donanteExistente instanceof PersonaJuridica)) {
      ctx.status(HttpStatus.BAD_REQUEST).result("El ID no corresponde a una Persona Jurídica.");
      return;
    }

    PersonaJuridica persona = (PersonaJuridica) donanteExistente;

    persona.actualizarDatos(request.getMail(), request.getNumeroDocumento(), request.getTipoDeDocumento());
    persona.setRazonSocial(request.getRazonSocial());
    persona.setTipo(request.getTipo());
    persona.setRubro(request.getRubro());

    ctx.status(HttpStatus.OK).result("Persona Jurídica actualizada exitosamente.");
  }
}