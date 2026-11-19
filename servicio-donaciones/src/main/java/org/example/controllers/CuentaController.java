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

      ContactoController.guardarDatos(ctx, em, cuenta);

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
    modelo.putAll(modeloPerfil(ctx, cuenta, false));

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

  public void editar(Context ctx) {
    var usuario = LogtoController.usuario(ctx);
    CuentaWeb cuenta = buscar(ctx);

    if (!org.example.seguridad.SesionWeb.esAdmin(usuario)
        && (cuenta == null || !cuenta.completa())) {
      continuar(ctx);
      return;
    }

    mostrarEdicion(ctx, "");
  }

  public void guardarEdicion(Context ctx) {
    var usuario = LogtoController.usuario(ctx);
    LogtoController.validarCsrf(ctx);

    EntityManager em = proveedor.get();
    var tx = em.getTransaction();

    try {
      tx.begin();

      CuentaWeb cuenta = em.find(
          CuentaWeb.class,
          CuentaWeb.clave(usuario),
          javax.persistence.LockModeType.PESSIMISTIC_WRITE
      );

      boolean admin = org.example.seguridad.SesionWeb.esAdmin(usuario);

      if (!admin && (cuenta == null || !cuenta.completa())) {
        throw new BadRequestResponse("Primero completá tu registro.");
      }

      if (!admin) {
        if ("ENTIDAD".equals(cuenta.rol)) {
          String razonSocial = campo(ctx, "razonSocial");

          cuenta.entidad.setRazonSocial(razonSocial);
          cuenta.nombre = razonSocial;

        } else if ("DONANTE".equals(cuenta.rol)) {
          actualizarDonante(ctx, cuenta, em);

        } else {
          throw new BadRequestResponse("El perfil necesita revisión.");
        }
      }

      ContactoController.guardarDatos(ctx, em, cuenta);

      tx.commit();

      if (!admin) {
        ctx.attribute("cuentaWeb", cuenta);
      }

      ctx.redirect("/cuenta/perfil?guardado=1");

    } catch (BadRequestResponse | IllegalArgumentException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      em.clear();

      if (!org.example.seguridad.SesionWeb.esAdmin(usuario)) {
        ctx.attribute("cuentaWeb", buscar(ctx));
      }

      ctx.status(400);

      mostrarEdicion(
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

  private void actualizarDonante(
      Context ctx,
      CuentaWeb cuenta,
      EntityManager em
  ) {
    Donante donante = (Donante) org.hibernate.Hibernate.unproxy(
        cuenta.donante
    );

    String documento = campo(ctx, "documento", 30);

    long repetidos = em.createQuery(
            "SELECT COUNT(d) FROM Donante d "
                + "WHERE d.numeroDocumento = :documento "
                + "AND d.id <> :id",
            Long.class
        )
        .setParameter("documento", documento)
        .setParameter("id", donante.getId())
        .getSingleResult();

    if (repetidos > 0) {
      throw new BadRequestResponse(
          "Ya existe otro donante con ese documento."
      );
    }

    // El subtipo se obtiene de la base, nunca del formulario.
    if (donante instanceof PersonaHumana persona) {
      String nombre = campo(ctx, "nombre", 80);
      String apellido = campo(ctx, "apellido", 80);

      int edad;

      try {
        edad = Integer.parseInt(campo(ctx, "edad"));
      } catch (NumberFormatException e) {
        throw new BadRequestResponse("La edad debe ser un número entero.");
      }

      if (edad < 0 || edad > 120) {
        throw new BadRequestResponse("Edad fuera de rango.");
      }

      persona.setNombre(nombre);
      persona.setApellido(apellido);
      persona.setEdad(edad);
      persona.setGenero(Genero.valueOf(campo(ctx, "genero")));
      persona.setDireccion(campo(ctx, "direccion"));

      cuenta.nombre = nombre + " " + apellido;

    } else if (donante instanceof PersonaJuridica persona) {
      String razonSocial = campo(ctx, "razonSocial");

      persona.setRazonSocial(razonSocial);
      persona.setTipo(
          TipoOrganizacion.valueOf(campo(ctx, "organizacion"))
      );
      persona.setRubro(campo(ctx, "rubro"));

      cuenta.nombre = razonSocial;

    } else {
      throw new BadRequestResponse("Tipo de donante no reconocido.");
    }

    donante.actualizarDatos(
        donante.getMail(),
        documento,
        donante.getTipoDeDocumento()
    );
  }

  private void mostrarEdicion(Context ctx, String error) {
    Map<String, Object> modelo = modeloPerfil(ctx, buscar(ctx), true);

    modelo.put("error", Objects.toString(error, ""));
    modelo.put(
        "guardado",
        error.isBlank() && "1".equals(ctx.queryParam("guardado"))
    );

    ctx.render("mi-perfil.hbs", modelo);
  }

  private Map<String, Object> modeloPerfil(
      Context ctx,
      CuentaWeb cuenta,
      boolean edicion
  ) {
    var usuario = LogtoController.usuario(ctx);

    boolean admin = org.example.seguridad.SesionWeb.esAdmin(usuario);
    boolean entidad = !admin
        && cuenta != null
        && "ENTIDAD".equals(cuenta.rol);

    Map<String, Object> perfil = new HashMap<>();

    for (String campo : List.of(
        "nombre", "apellido", "edad", "direccion",
        "razonSocial", "rubro", "documento"
    )) {
      perfil.put(campo, "");
    }

    String persona = "humana";
    String genero = "OTRO";
    String organizacion = "EMPRESA";

    if (entidad && cuenta.entidad != null) {
      perfil.put("razonSocial", cuenta.entidad.getRazonSocial());

    } else if (!admin && cuenta != null && cuenta.donante != null) {
      Donante donante = (Donante) org.hibernate.Hibernate.unproxy(
          cuenta.donante
      );

      perfil.put("documento", donante.getNumeroDocumento());

      if (donante instanceof PersonaHumana humana) {
        perfil.put("nombre", humana.getNombre());
        perfil.put("apellido", humana.getApellido());
        perfil.put("edad", humana.getEdad());
        perfil.put("direccion", humana.getDireccion());

        genero = Objects.toString(humana.getGenero(), "OTRO");

      } else if (donante instanceof PersonaJuridica juridica) {
        persona = "juridica";

        perfil.put("razonSocial", juridica.getRazonSocial());
        perfil.put("rubro", juridica.getRubro());

        organizacion = Objects.toString(juridica.getTipo(), "EMPRESA");
      }
    }

    // Recuperar los datos enviados cuando hay un error.
    for (String campo : List.of(
        "nombre", "apellido", "edad", "direccion",
        "razonSocial", "rubro", "documento"
    )) {
      if (ctx.formParam(campo) != null) {
        perfil.put(campo, ctx.formParam(campo));
      }
    }

    if (!edicion && "juridica".equals(ctx.formParam("persona"))) {
      persona = "juridica";
    }

    if (ctx.formParam("genero") != null) {
      genero = ctx.formParam("genero");
    }

    if (ctx.formParam("organizacion") != null) {
      organizacion = ctx.formParam("organizacion");
    }

    perfil.put("humana", "humana".equals(persona));
    perfil.put("juridica", "juridica".equals(persona));
    perfil.put("generos", opciones(Genero.values(), genero));
    perfil.put(
        "organizaciones",
        opciones(TipoOrganizacion.values(), organizacion)
    );

    Map<String, Object> modelo = new HashMap<>();
    modelo.put("perfil", perfil);
    modelo.put("admin", admin);
    modelo.put("entidad", entidad);
    modelo.put("edicion", edicion);
    modelo.put(
        "contacto",
        ContactoController.modelo(ctx, proveedor.get(), cuenta)
    );

    return modelo;
  }

  private static List<Map<String, Object>> opciones(
      Enum<?>[] valores,
      String seleccionado
  ) {
    List<Map<String, Object>> opciones = new ArrayList<>();

    for (Enum<?> valor : valores) {
      String etiqueta = valor.name()
          .toLowerCase(Locale.ROOT)
          .replace('_', ' ');

      etiqueta = etiqueta.substring(0, 1).toUpperCase(Locale.ROOT)
          + etiqueta.substring(1);

      if ("OTRO".equals(valor.name())) {
        etiqueta = "Otro / Prefiero no indicar";
      }

      opciones.add(Map.of(
          "valor", valor.name(),
          "etiqueta", etiqueta,
          "seleccionado", valor.name().equals(seleccionado)
      ));
    }

    return opciones;
  }
}