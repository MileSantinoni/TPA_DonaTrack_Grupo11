package org.example.controllers;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.example.api.donaciones.dto.EntidadBeneficiariaRequest;
import org.example.api.donaciones.dto.PersonaHumanaRequest;
import org.example.api.donaciones.dto.PersonaJuridicaRequest;
import org.example.Repositorios.RepositorioDonantes;
import org.example.Repositorios.RepositorioEntidadesBeneficiarias;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.TipoDocumento;
import org.example.dominio.donante.TipoOrganizacion;
import org.example.dominio.beneficiario.EntidadBeneficiaria;

import java.util.HashMap;
import java.util.Map;

public class RegistrationController {

    private final RepositorioDonantes repoDonantes;
    private final RepositorioEntidadesBeneficiarias repoEntidades;

    public RegistrationController(RepositorioDonantes repoDonantes, RepositorioEntidadesBeneficiarias repoEntidades) {
        this.repoDonantes = repoDonantes;
        this.repoEntidades = repoEntidades;
    }

    public void registroDonante(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("titulo", "Registrate como Donante");
        model.put("tipo", "donante");
        ctx.render("registrationPage.hbs", model);
    }

    public void registroDonanteJuridica(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("titulo", "Registrate como Donante (Persona Jurídica)");
        model.put("tipo", "juridica");
        ctx.render("registrationPage.hbs", model);
    }

    public void registroEntidad(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("titulo", "Registrate como Entidad Beneficiaria");
        model.put("tipo", "entidad");
        ctx.render("registrationPage.hbs", model);
    }

    public void crearPersonaHumana(Context ctx) {
        PersonaHumanaRequest request = ctx.bodyAsClass(PersonaHumanaRequest.class);

        if (repoDonantes.existeMail(request.getMail())) {
            ctx.status(HttpStatus.BAD_REQUEST).result("El mail ya está registrado.");
            return;
        }

        org.example.dominio.donante.PersonaHumana nuevoDonante = new org.example.dominio.donante.PersonaHumana(
                request.getMail(),
                request.getNumeroDocumento(),
                request.getTipoDeDocumento(),
                request.getNombre(),
                request.getApellido(),
                request.getEdad(),
                request.getGenero(),
                request.getDireccion()
        );

        repoDonantes.agregar(nuevoDonante);
        ctx.status(HttpStatus.CREATED).result("Donante creado exitosamente.");
    }

    public void crearPersonaJuridica(Context ctx) {
        PersonaJuridicaRequest request = ctx.bodyAsClass(PersonaJuridicaRequest.class);

        if (repoDonantes.existeMail(request.getMail())) {
            ctx.status(HttpStatus.BAD_REQUEST).result("El mail ya está registrado.");
            return;
        }

        org.example.dominio.donante.PersonaJuridica nuevoDonante = new org.example.dominio.donante.PersonaJuridica(
                request.getMail(),
                request.getNumeroDocumento(),
                request.getTipoDeDocumento(),
                request.getRazonSocial(),
                request.getTipo(),
                request.getRubro()
        );

        repoDonantes.agregar(nuevoDonante);
        ctx.status(HttpStatus.CREATED).result("Persona Jurídica creada exitosamente.");
    }

    public void crearEntidad(Context ctx) {
        EntidadBeneficiariaRequest request = ctx.bodyAsClass(EntidadBeneficiariaRequest.class);

        EntidadBeneficiaria nuevaEntidad = new EntidadBeneficiaria(
                request.getRazonSocial(),
                request.getDireccion(),
                request.getTelefono()
        );

        repoEntidades.agregar(nuevaEntidad);
        ctx.status(HttpStatus.CREATED).result("Entidad Beneficiaria creada exitosamente.");
    }
}
