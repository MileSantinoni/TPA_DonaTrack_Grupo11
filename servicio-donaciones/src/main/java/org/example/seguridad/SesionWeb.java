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

  public static CuentaWeb cuenta(Context ctx) {
    CuentaWeb cuenta = ctx.attribute("cuentaWeb");

    if (cuenta == null) {
      throw new ForbiddenResponse(
          "Completá tu perfil desde la página de inicio."
      );
    }

    return cuenta;
  }

  public static void configurar(
      Javalin app,
      Supplier<EntityManager> proveedor
  ) {
    app.exception(UnauthorizedResponse.class, (e, ctx) -> {
      boolean pagina = ctx.method() == HandlerType.GET
          && Objects.toString(ctx.header("Accept"), "")
          .contains("text/html");

      if (pagina) {
        ctx.redirect("/login");
      } else {
        ctx.status(401).json(
            Map.of("mensaje", "Iniciá sesión para continuar.")
        );
      }
    });

    app.beforeMatched(ctx -> {
      Usuario usuario = opcional(ctx);

      if (usuario != null) {
        ctx.header("Cache-Control", "no-store");

        CuentaWeb cuenta = proveedor.get().find(
            CuentaWeb.class,
            CuentaWeb.clave(usuario)
        );

        ctx.attribute("cuentaWeb", cuenta);
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

      if (!rol.equals(cuenta(ctx).rol)) {
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

    if (prefijo(ruta, "/mi-entidad")) {
      return "ENTIDAD";
    }

    return null;
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
      URI recibido = URI.create(Objects.toString(origen, ""));
      URI esperado = URI.create(
          System.getenv("LOGTO_REDIRECT_URI")
      );

      if (!Objects.equals(
          recibido.getScheme(),
          esperado.getScheme()
      )
          || !Objects.equals(
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

    String nombre = cuenta != null
        ? cuenta.nombre
        : usuario.nombre();

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

    String titulo = "Completá tu cuenta de DonaTrack";
    String descripcion =
        "Elegí tu perfil y completá tus datos para empezar.";
    String boton = "Completar mi perfil";
    String destino = "/cuenta/completar";

    if (cuenta != null) {
      switch (cuenta.rol) {
        case "ADMIN" -> {
          titulo = "Administrá las donaciones y entregas";
          descripcion =
              "Organizá las donaciones, las personas donantes "
                  + "y los camiones desde tu panel.";
          boton = "Ir al panel administrador";
          destino = "/administracion/donaciones";
        }

        case "DONANTE" -> {
          titulo = "Cada donación cuenta";
          descripcion =
              "Revisá tus donaciones y conocé "
                  + "el recorrido de tu ayuda.";
          boton = "Ver mis donaciones";
          destino = "/mis-donaciones";
        }

        case "ENTIDAD" -> {
          titulo = "Gestioná las necesidades de tu entidad";
          descripcion =
              "Actualizá tus necesidades y seguí "
                  + "las entregas destinadas a tu entidad.";
          boton = "Ir a mi entidad";
          destino = "/mi-entidad";
        }
      }
    }

    sesion.put("titulo", titulo);
    sesion.put("descripcion", descripcion);
    sesion.put("boton", boton);
    sesion.put("destino", destino);
  }
}