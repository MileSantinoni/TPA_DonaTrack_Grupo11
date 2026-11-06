package org.example.controllers;

import io.javalin.http.*;
import java.util.*;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donante.*;
import org.example.seguridad.CuentaWeb;

public class CuentaController {

  private final Supplier<EntityManager> proveedor;
  private final LandingController landing;

  public CuentaController(
      Supplier<EntityManager> proveedor,
      LandingController landing
  ) {
    this.proveedor = proveedor;
    this.landing = landing;
  }

  public void continuar(Context ctx) {
    ctx.redirect(buscar(ctx) == null ? "/cuenta/completar" : "/");
  }

  public void formulario(Context ctx) {
    if (buscar(ctx) != null) {
      ctx.redirect("/");
      return;
    }

    String tipo = ctx.queryParam("tipo");

    if (tipo != null) {
      validarTipo(tipo);
      ctx.sessionAttribute("registro.tipo", tipo);
    }

    mostrar(ctx, "");
  }

  public void guardar(Context ctx) {
    var usuario = LogtoController.usuario(ctx);
    LogtoController.validarCsrf(ctx);

    if (buscar(ctx) != null) {
      ctx.redirect("/");
      return;
    }

    if (!usuario.emailVerificado() || usuario.email().isBlank()) {
      ctx.status(400);
      mostrar(
          ctx,
          "Verificá tu correo en Logto y volvé a iniciar sesión."
      );
      return;
    }

    EntityManager em = proveedor.get();

    try {
      String tipo = ctx.sessionAttribute("registro.tipo");
      validarTipo(tipo);

      CuentaWeb cuenta = new CuentaWeb();
      cuenta.id = CuentaWeb.clave(usuario);
      cuenta.rol = tipo;

      em.getTransaction().begin();

      try {
        if ("ENTIDAD".equals(tipo)) {
          cuenta.nombre = campo(ctx, "razonSocial");

          cuenta.entidad = new EntidadBeneficiaria(
              cuenta.nombre,
              campo(ctx, "direccion"),
              campo(ctx, "telefono")
          );

          em.persist(cuenta.entidad);
        } else {
          crearDonante(ctx, usuario, cuenta, em);
        }

        em.persist(cuenta);
        em.getTransaction().commit();

      } catch (RuntimeException e) {
        if (em.getTransaction().isActive()) {
          em.getTransaction().rollback();
        }

        em.clear();
        throw e;
      }

      ctx.sessionAttribute("registro.tipo", null);
      ctx.redirect("/");

    } catch (BadRequestResponse | IllegalArgumentException e) {
      ctx.status(400);

      mostrar(
          ctx,
          e instanceof BadRequestResponse
              ? e.getMessage()
              : "Revisá los datos del formulario."
      );
    }
  }

  private void crearDonante(
      Context ctx,
      LogtoController.Usuario usuario,
      CuentaWeb cuenta,
      EntityManager em
  ) {
    String documento = campo(ctx, "documento");

    long existentes = em.createQuery(
            "SELECT COUNT(d) FROM Donante d "
                + "WHERE LOWER(d.mail) = :email "
                + "OR d.numeroDocumento = :documento",
            Long.class
        )
        .setParameter(
            "email",
            usuario.email().toLowerCase(Locale.ROOT)
        )
        .setParameter("documento", documento)
        .getSingleResult();

    if (existentes > 0) {
      throw new BadRequestResponse(
          "Ya existe un donante con ese correo o documento. "
              + "Solicitá al administrador que vincule tu cuenta."
      );
    }

    String persona = campo(ctx, "persona");

    if ("humana".equals(persona)) {
      int edad;

      try {
        edad = Integer.parseInt(campo(ctx, "edad"));
      } catch (NumberFormatException e) {
        throw new BadRequestResponse(
            "La edad debe ser un número entero."
        );
      }

      if (edad < 0 || edad > 120) {
        throw new BadRequestResponse("Edad fuera de rango.");
      }

      String nombre = campo(ctx, "nombre", 80);
      String apellido = campo(ctx, "apellido", 80);

      cuenta.nombre = nombre + " " + apellido;

      cuenta.donante = new PersonaHumana(
          usuario.email(),
          documento,
          TipoDocumento.DNI,
          nombre,
          apellido,
          edad,
          Genero.valueOf(campo(ctx, "genero")),
          campo(ctx, "direccion")
      );

    } else if ("juridica".equals(persona)) {
      cuenta.nombre = campo(ctx, "razonSocial");

      cuenta.donante = new PersonaJuridica(
          usuario.email(),
          documento,
          TipoDocumento.CUIT,
          cuenta.nombre,
          TipoOrganizacion.valueOf(
              campo(ctx, "organizacion")
          ),
          campo(ctx, "rubro")
      );

    } else {
      throw new BadRequestResponse("Tipo de persona inválido.");
    }

    cuenta.donante.activar();
    em.persist(cuenta.donante);
  }

  private CuentaWeb buscar(Context ctx) {
    return proveedor.get().find(
        CuentaWeb.class,
        CuentaWeb.clave(LogtoController.usuario(ctx))
    );
  }

  private void mostrar(Context ctx, String error) {
    String tipo = ctx.sessionAttribute("registro.tipo");

    Map<String, Object> modelo = new HashMap<>();
    modelo.put("mostrarPerfil", true);
    modelo.put("elegir", tipo == null);
    modelo.put("entidad", "ENTIDAD".equals(tipo));
    modelo.put("error", Objects.toString(error, ""));

    landing.renderizar(ctx, modelo);
  }

  private static void validarTipo(String tipo) {
    if (!"DONANTE".equals(tipo) && !"ENTIDAD".equals(tipo)) {
      throw new BadRequestResponse(
          "Elegí Donante o Entidad beneficiaria."
      );
    }
  }

  private static String campo(Context ctx, String nombre) {
    return campo(ctx, nombre, 180);
  }

  private static String campo(
      Context ctx,
      String nombre,
      int maximo
  ) {
    String valor = Objects.toString(
        ctx.formParam(nombre),
        ""
    ).trim();

    if (valor.isEmpty() || valor.length() > maximo) {
      throw new BadRequestResponse(
          "Revisá el campo " + nombre + "."
      );
    }

    return valor;
  }
}