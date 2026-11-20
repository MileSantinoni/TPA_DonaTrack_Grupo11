package org.example.seguridad;

import io.javalin.Javalin;
import io.javalin.http.*;

import java.net.URI;
import java.util.*;
import java.util.function.Supplier;

import javax.persistence.EntityManager;

import org.example.controllers.LogtoController;
import org.example.controllers.LogtoController.Usuario;

public final class SesionWeb {

  private SesionWeb() {
  }

  public static Usuario opcional(Context ctx) {
    try {
      return LogtoController.usuario(ctx);
    } catch (UnauthorizedResponse e) {
      return null;
    }
  }

  public static boolean esAdmin(Usuario usuario) {
    return usuario != null
        && usuario.roles() != null
        && usuario.roles().contains("ADMIN");
  }

  public static CuentaWeb cuenta(Context ctx) {
    CuentaWeb cuenta = ctx.attribute("cuentaWeb");

    if (cuenta == null) {
      throw new ForbiddenResponse("Completá el registro.");
    }

    return cuenta;
  }

  public static String destino(Context ctx) {
    Usuario usuario = LogtoController.usuario(ctx);

    if (esAdmin(usuario)) {
      return "/administracion/donaciones";
    }

    CuentaWeb cuenta = ctx.attribute("cuentaWeb");

    if (cuenta == null) {
      return "/auth/registro";
    }

    if (!cuenta.completa()) {
      return "/cuenta/completar";
    }

    return "ENTIDAD".equals(cuenta.rol)
        ? "/mi-entidad"
        : "/mis-donaciones";
  }

  public static void registrarTipo(
      Context ctx,
      Usuario usuario,
      String tipo
  ) {
    if (esAdmin(usuario) || tipo == null) {
      return;
    }

    if (!Set.of("DONANTE", "ENTIDAD").contains(tipo)) {
      throw new BadRequestResponse(
          "Tipo de registro inválido."
      );
    }

    EntityManager em = ctx.attribute("cuenta.em");

    if (em == null) {
      throw new IllegalStateException(
          "Falta el contexto de persistencia."
      );
    }

    String id = CuentaWeb.clave(usuario);

    // Una cuenta existente conserva su elección original.
    if (em.find(CuentaWeb.class, id) != null) {
      return;
    }

    var tx = em.getTransaction();
    tx.begin();

    try {
      CuentaWeb cuenta = new CuentaWeb();

      cuenta.id = id;
      cuenta.rol = tipo;
      cuenta.nombre = usuario.nombre().isBlank()
          ? usuario.email()
          : usuario.nombre();

      if (cuenta.nombre.isBlank()) {
        cuenta.nombre = "Registro pendiente";
      }

      em.persist(cuenta);
      tx.commit();

      ctx.attribute("cuentaWeb", cuenta);
    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      em.clear();
      throw e;
    }
  }

  public static void configurar(
      Javalin app,
      Supplier<EntityManager> proveedor
  ) {
    app.exception(UnauthorizedResponse.class, (e, ctx) -> {
      if (pagina(ctx)) {
        if (Set.of("/mi-entidad", "/mis-donaciones")
            .contains(ctx.path())) {

          String entrega = ctx.queryParam("entrega");

          if (entrega != null) {
            try {
              ctx.sessionAttribute(
                  "notificaciones.entregaPendiente",
                  UUID.fromString(entrega).toString()
              );
            } catch (IllegalArgumentException ignorada) {
              // Nunca guardamos una URL enviada por el navegador.
            }
          }
        }

        ctx.redirect("/login");
      } else {
        ctx.status(401).json(Map.of(
            "mensaje",
            "Iniciá sesión para continuar."
        ));
      }
    });

    app.beforeMatched(ctx -> {
      ctx.attribute("cuenta.em", proveedor.get());

      // Estas rutas no utilizan la sesión web.
      // Conservan sus propios controles de integración.
      if (ctx.path().equals("/graphql")
          || prefijo(ctx.path(), "/interno")
          || ctx.path().equals("/health")
          || prefijo(ctx.path(), "/assets")) {
        return;
      }

      Usuario usuario = opcional(ctx);
      CuentaWeb cuenta = null;

      if (usuario != null) {
        ctx.header("Cache-Control", "no-store");

        if (esAdmin(usuario)) {
          // Representación para esta petición.
          // ADMIN no se guarda como autorización local.
          cuenta = new CuentaWeb();
          cuenta.id = CuentaWeb.clave(usuario);
          cuenta.rol = "ADMIN";
          cuenta.nombre = usuario.nombre().isBlank()
              ? usuario.email()
              : usuario.nombre();
        } else {
          cuenta = proveedor.get().find(
              CuentaWeb.class,
              CuentaWeb.clave(usuario)
          );
        }

        ctx.attribute("cuentaWeb", cuenta);

        boolean pendiente = !esAdmin(usuario)
            && (cuenta == null || !cuenta.completa());

        boolean permitida =
            prefijo(ctx.path(), "/auth")
                || ctx.path().equals("/login")
                || ctx.path().equals("/logout")
                || prefijo(ctx.path(), "/cuenta");

        if (pendiente && !permitida) {
          String destino = cuenta == null
              ? "/auth/registro"
              : "/cuenta/completar";

          if (pagina(ctx)) {
            ctx.redirect(destino);
            ctx.skipRemainingHandlers();
          } else {
            throw new ForbiddenResponse(
                "Completá tu registro para continuar."
            );
          }

          return;
        }
      }

      String rol = rolRequerido(ctx.path());

      if (rol == null) {
        return;
      }

      if (usuario == null) {
        throw new UnauthorizedResponse(
            "Iniciá sesión para continuar."
        );
      }

      if (cuenta == null
          || !rol.equals(cuenta.rol)
          || ("ADMIN".equals(rol) && !esAdmin(usuario))) {

        throw new ForbiddenResponse(
            "Tu perfil no tiene acceso a esta pantalla."
        );
      }

      if (!Set.of("GET", "HEAD", "OPTIONS")
          .contains(ctx.method().name())) {
        validarOrigen(ctx);
      }
    });
  }

  private static boolean pagina(Context ctx) {
    return ctx.method() == HandlerType.GET
        && Objects.toString(ctx.header("Accept"), "")
        .contains("text/html");
  }

  private static String rolRequerido(String ruta) {
    for (String base : List.of(
        "/administracion",
        "/donantes",
        "/donaciones",
        "/entidades",
        "/asignaciones",
        "/registros-donacion"
    )) {
      if (prefijo(ruta, base)) {
        return "ADMIN";
      }
    }

    if (prefijo(ruta, "/mis-donaciones")
        || prefijo(ruta, "/entidades-beneficiarias")) {
      return "DONANTE";
    }

    return prefijo(ruta, "/mi-entidad")
        ? "ENTIDAD"
        : null;
  }

  private static boolean prefijo(String ruta, String base) {
    return ruta.equals(base) || ruta.startsWith(base + "/");
  }

  private static void validarOrigen(Context ctx) {
    String origen = ctx.header("Origin");

    if (origen == null) {
      origen = ctx.header("Referer");
    }

    try {
      URI recibido = URI.create(
          Objects.toString(origen, "")
      );

      URI esperado = URI.create(
          System.getenv("LOGTO_REDIRECT_URI")
      );

      if (!Objects.equals(
          recibido.getScheme(),
          esperado.getScheme()
      ) || !Objects.equals(
          recibido.getAuthority(),
          esperado.getAuthority()
      )) {
        throw new IllegalArgumentException();
      }
    } catch (Exception e) {
      throw new ForbiddenResponse(
          "Origen de la solicitud inválido."
      );
    }
  }

  public static void modelo(
      Context ctx,
      Map<String, Object> modelo
  ) {
    Map<String, Object> sesion = new HashMap<>();
    modelo.put("sesion", sesion);

    Usuario usuario = opcional(ctx);

    if (usuario == null) {
      return;
    }

    CuentaWeb cuenta = ctx.attribute("cuentaWeb");

    String nombre = cuenta == null
        ? usuario.nombre()
        : cuenta.nombre;

    if (nombre == null || nombre.isBlank()) {
      nombre = usuario.email();
    }

    if (nombre == null || nombre.isBlank()) {
      nombre = "Mi cuenta";
    }

    sesion.put("autenticado", true);
    sesion.put("nombre", nombre);
    sesion.put(
        "inicial",
        nombre.substring(0, 1).toUpperCase(Locale.ROOT)
    );

    sesion.put("csrf", LogtoController.csrf(ctx));
    sesion.put("destino", destino(ctx));
    String rol = esAdmin(usuario)
        ? "ADMIN"
        : cuenta == null ? "" : cuenta.rol;

    sesion.put("rolNombre", switch (rol) {
      case "ADMIN" -> "Administración";
      case "ENTIDAD" -> "Entidad beneficiaria";
      case "DONANTE" -> "Donante";
      default -> "Registro pendiente";
    });

    sesion.put("perfilAvisos", switch (rol) {
      case "ADMIN" -> "admin";
      case "ENTIDAD" -> "beneficiaria";
      case "DONANTE" -> "donante";
      default -> "";
    });

    sesion.put("accesoNombre", switch (rol) {
      case "ADMIN" -> "Administración";
      case "ENTIDAD" -> "Mi entidad";
      case "DONANTE" -> "Mis donaciones";
      default -> "Completar registro";
    });
    sesion.put("titulo", "Tu espacio en DonaTrack");
    sesion.put(
        "descripcion",
        "Consultá y gestioná tu actividad."
    );

    sesion.put(
        "boton",
        esAdmin(usuario)
            ? "Ir a Administración"
            : "Ir a mi panel"
    );
  }
}