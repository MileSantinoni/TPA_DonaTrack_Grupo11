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
    ctx.redirect(org.example.seguridad.SesionWeb.destino(ctx));
  }

  public void formulario(Context ctx) {
    var usuario = LogtoController.usuario(ctx);
    CuentaWeb cuenta = buscar(ctx);

    if (org.example.seguridad.SesionWeb.esAdmin(usuario)
        || cuenta != null && cuenta.completa()) {

      continuar(ctx);
      return;
    }

    if (cuenta == null) {
      ctx.redirect("/auth/registro");
      return;
    }

    mostrar(ctx, "");
  }

  public void guardar(Context ctx) {
    var usuario = LogtoController.usuario(ctx);
    LogtoController.validarCsrf(ctx);

    if (org.example.seguridad.SesionWeb.esAdmin(usuario)) {
      continuar(ctx);
      return;
    }

    EntityManager em = proveedor.get();
    var tx = em.getTransaction();

    try {
      tx.begin();

      CuentaWeb cuenta = em.find(
          CuentaWeb.class,
          CuentaWeb.clave(usuario),
          javax.persistence.LockModeType.PESSIMISTIC_WRITE
      );

      if (cuenta == null) {
        throw new BadRequestResponse(
            "Primero elegí el botón de registro."
        );
      }

      if (cuenta.completa()) {
        tx.commit();
        continuar(ctx);
        return;
      }

      if (!usuario.emailVerificado()
          || usuario.email().isBlank()) {

        throw new BadRequestResponse(
            "Verificá tu correo en Logto, "
                + "cerrá sesión y volvé a ingresar."
        );
      }

      validarTipo(cuenta.rol);

      if ("ENTIDAD".equals(cuenta.rol)) {
        cuenta.nombre = campo(ctx, "razonSocial");

        cuenta.entidad = new EntidadBeneficiaria(
            cuenta.nombre,
            "",
            campo(ctx, "telefono", 40)
        );

        em.persist(cuenta.entidad);

      } else {
        crearDonante(ctx, usuario, cuenta, em);
      }

      tx.commit();

      ctx.attribute("cuentaWeb", cuenta);
      continuar(ctx);

    } catch (BadRequestResponse | IllegalArgumentException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      em.clear();
      ctx.attribute("cuentaWeb", buscar(ctx));
      ctx.status(400);

      mostrar(
          ctx,
          e instanceof BadRequestResponse
              ? e.getMessage()
              : "Revisá los datos del formulario."
      );

    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
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
    CuentaWeb cuenta = buscar(ctx);

    if (cuenta == null) {
      ctx.redirect("/auth/registro");
      return;
    }

    Map<String, Object> modelo = new HashMap<>();

    modelo.put("mostrarPerfil", true);
    modelo.put("entidad", "ENTIDAD".equals(cuenta.rol));
    modelo.put("error", Objects.toString(error, ""));

    if (!List.of("DONANTE", "ENTIDAD").contains(cuenta.rol)) {
      modelo.put("bloqueado", true);

      modelo.put(
          "error",
          "Esta cuenta necesita revisión. "
              + "Si era administradora, asignale ADMIN en Logto "
              + "y volvé a ingresar."
      );
    }

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