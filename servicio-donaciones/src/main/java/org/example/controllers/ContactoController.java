package org.example.controllers;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;

import org.example.seguridad.ContactoCuenta;
import org.example.seguridad.CuentaWeb;
import org.example.seguridad.SesionWeb;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

public class ContactoController {

  private static final PhoneNumberUtil TELEFONOS =
      PhoneNumberUtil.getInstance();

  private static final Locale ESPANOL = Locale.forLanguageTag("es");

  private final Supplier<EntityManager> proveedor;

  public ContactoController(Supplier<EntityManager> proveedor) {
    this.proveedor = proveedor;
  }

  public void formulario(Context ctx) {
    CuentaWeb cuenta = cuentaAutorizada(ctx);
    mostrar(ctx, cuenta, "");
  }

  public void guardar(Context ctx) {
    CuentaWeb cuenta = cuentaAutorizada(ctx);
    LogtoController.validarCsrf(ctx);

    EntityManager em = proveedor.get();
    var tx = em.getTransaction();

    try {
      tx.begin();

      guardarDatos(ctx, em, cuenta);

      tx.commit();
      ctx.redirect("/cuenta/perfil?guardado=1");

    } catch (BadRequestResponse e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      em.clear();
      ctx.status(400);
      mostrar(ctx, cuentaAutorizada(ctx), e.getMessage());

    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
    }
  }

  /*
   * Usa la transacción del llamador.
   * También se invoca desde CuentaController durante el registro.
   */
  public static void guardarDatos(
      Context ctx,
      EntityManager em,
      CuentaWeb cuenta
  ) {
    var usuario = LogtoController.usuario(ctx);

    String email = Objects.toString(usuario.email(), "").trim();

    if (!usuario.emailVerificado() || email.isBlank()) {
      throw new BadRequestResponse(
          "Verificá tu correo en Logto y volvé a iniciar sesión."
      );
    }

    if (email.length() > 320) {
      throw new BadRequestResponse("El correo es demasiado largo.");
    }

    ContactoCuenta.Canal canal;

    try {
      canal = ContactoCuenta.Canal.valueOf(
          valor(ctx, "canalNotificacion", "")
      );
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse(
          "Elegí un medio de notificación válido."
      );
    }

    String pais = valor(ctx, "paisTelefono", "")
        .toUpperCase(Locale.ROOT);

    if (!TELEFONOS.getSupportedRegions().contains(pais)) {
      throw new BadRequestResponse("Elegí un país válido.");
    }

    boolean esEntidad = cuenta != null && cuenta.entidad != null;

    String telefono = normalizar(
        valor(ctx, "telefono", ""),
        pais,
        esEntidad || canal != ContactoCuenta.Canal.MAIL
    );

    String id = CuentaWeb.clave(usuario);
    ContactoCuenta contacto = em.find(ContactoCuenta.class, id);

    boolean nuevo = contacto == null;

    if (nuevo) {
      contacto = new ContactoCuenta();
      contacto.id = id;
    }

// Completamos los campos obligatorios antes de persistir.
    contacto.email = email;
    contacto.pais = pais;
    contacto.telefono = telefono;
    contacto.canal = canal;
    contacto.actualizado = Instant.now();
    contacto.recibeAvisosAdministracion = SesionWeb.esAdmin(usuario);

    if (nuevo) {
      em.persist(contacto);
    }
    if (esEntidad) {
      cuenta.entidad.setTelefono(telefono);
    }
  }

  public static Map<String, Object> modelo(
      Context ctx,
      EntityManager em,
      CuentaWeb cuenta
  ) {
    var usuario = LogtoController.usuario(ctx);

    ContactoCuenta contacto = em.find(
        ContactoCuenta.class,
        CuentaWeb.clave(usuario)
    );

    String pais = valor(
        ctx,
        "paisTelefono",
        contacto == null ? "AR" : contacto.pais
    );

    String telefonoAnterior = contacto == null
        ? ""
        : Objects.toString(contacto.telefono, "");

    if (contacto == null && cuenta != null && cuenta.entidad != null) {
      telefonoAnterior = Objects.toString(
          cuenta.entidad.getTelefono(),
          ""
      );
    }

    String canal = valor(
        ctx,
        "canalNotificacion",
        contacto == null ? "MAIL" : contacto.canal.name()
    );

    List<Map<String, Object>> paises = new ArrayList<>();

    for (String codigo : TELEFONOS.getSupportedRegions()) {
      String nombre = new Locale("", codigo)
          .getDisplayCountry(ESPANOL);

      paises.add(Map.of(
          "codigo", codigo,
          "nombre", nombre + " (+"
              + TELEFONOS.getCountryCodeForRegion(codigo) + ")",
          "seleccionado", codigo.equals(pais)
      ));
    }

    paises.sort(Comparator.comparing(
        p -> p.get("nombre").toString()
    ));

    Map<String, Object> modelo = new HashMap<>();
    modelo.put("email", Objects.toString(usuario.email(), ""));
    modelo.put("telefono", valor(ctx, "telefono", telefonoAnterior));
    modelo.put("paises", paises);
    modelo.put("mail", "MAIL".equals(canal));
    modelo.put("sms", "SMS".equals(canal));
    modelo.put("whatsapp", "WHATSAPP".equals(canal));

    modelo.put(
        "telefonoObligatorio",
        cuenta != null && "ENTIDAD".equals(cuenta.rol)
    );

    return modelo;
  }

  private CuentaWeb cuentaAutorizada(Context ctx) {
    var usuario = LogtoController.usuario(ctx);

    CuentaWeb cuenta = proveedor.get().find(
        CuentaWeb.class,
        CuentaWeb.clave(usuario)
    );

    if (!SesionWeb.esAdmin(usuario)
        && (cuenta == null || !cuenta.completa())) {
      throw new ForbiddenResponse(
          "Completá tu registro antes de editar el contacto."
      );
    }

    return cuenta;
  }

  private void mostrar(
      Context ctx,
      CuentaWeb cuenta,
      String error
  ) {
    Map<String, Object> vista = new HashMap<>();

    vista.put("contacto", modelo(ctx, proveedor.get(), cuenta));
    vista.put("error", Objects.toString(error, ""));
    vista.put(
        "guardado",
        error.isBlank() && "1".equals(ctx.queryParam("guardado"))
    );

    ctx.render("contacto-cuenta.hbs", vista);
  }

  private static String normalizar(
      String texto,
      String pais,
      boolean obligatorio
  ) {
    if (texto.isBlank()) {
      if (obligatorio) {
        throw new BadRequestResponse(
            "Ingresá un teléfono para continuar."
        );
      }

      return null;
    }

    if (texto.length() > 40 || !texto.matches("[+0-9() .-]+")) {
      throw new BadRequestResponse("Revisá el número de teléfono.");
    }

    try {
      var numero = TELEFONOS.parse(texto, pais);

      if (!TELEFONOS.isValidNumber(numero)
          || !TELEFONOS.isValidNumberForRegion(numero, pais)) {
        throw new BadRequestResponse(
            "El teléfono no es válido para el país seleccionado."
        );
      }

      return TELEFONOS.format(
          numero,
          PhoneNumberUtil.PhoneNumberFormat.E164
      );

    } catch (NumberParseException e) {
      throw new BadRequestResponse(
          "Ingresá el teléfono con su código de área."
      );
    }
  }

  private static String valor(
      Context ctx,
      String campo,
      String alternativa
  ) {
    String valor = ctx.formParam(campo);
    return valor == null ? alternativa : valor.trim();
  }
}