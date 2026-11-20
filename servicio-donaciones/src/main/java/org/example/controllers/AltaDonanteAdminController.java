package org.example.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.javalin.http.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import javax.persistence.EntityManager;

import org.example.dominio.donante.*;
import org.example.seguridad.*;

public class AltaDonanteAdminController {

  private final Supplier<EntityManager> proveedor;
  private final LogtoManagement logto;

  public AltaDonanteAdminController(
      Supplier<EntityManager> proveedor
  ) {
    this.proveedor = proveedor;
    this.logto = new LogtoManagement();
  }

  public void crear(Context ctx) {
    if (!SesionWeb.esAdmin(LogtoController.usuario(ctx))) {
      throw new ForbiddenResponse(
          "Sólo administración puede registrar donantes."
      );
    }

    String esperado = LogtoController.csrf(ctx);
    String recibido = ctx.header("X-CSRF-Token");

    if (esperado == null
        || recibido == null
        || !MessageDigest.isEqual(
        esperado.getBytes(StandardCharsets.UTF_8),
        recibido.getBytes(StandardCharsets.UTF_8)
    )) {
      throw new ForbiddenResponse(
          "El formulario venció. Recargá la página."
      );
    }

    JsonNode datos = ctx.bodyAsClass(JsonNode.class);

    if (datos == null || !datos.isObject()) {
      throw new BadRequestResponse(
          "Ingresá los datos del donante."
      );
    }

    String clase = texto(datos, "clase", 10, true);

    if (!clase.equals("humana") && !clase.equals("juridica")) {
      throw new BadRequestResponse(
          "Elegí el tipo de persona."
      );
    }

    String email = texto(datos, "mail", 255, true)
        .toLowerCase(Locale.ROOT);

    if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
      throw new BadRequestResponse(
          "Ingresá un correo válido."
      );
    }

    String documento = texto(
        datos, "numeroDocumento", 30, true
    );

    String pais = texto(datos, "paisTelefono", 2, true)
        .toUpperCase(Locale.ROOT);

    if (!PhoneNumberUtil.getInstance()
        .getSupportedRegions().contains(pais)) {
      throw new BadRequestResponse(
          "Elegí un país válido."
      );
    }

    ContactoCuenta.Canal canal = opcion(
        ContactoCuenta.Canal.class,
        texto(datos, "canalNotificacion", 20, true)
    );

    String telefono = ContactoController.normalizar(
        texto(datos, "telefono", 40, false),
        pais,
        canal != ContactoCuenta.Canal.MAIL
    );

    Donante donante;
    String nombre;

    if (clase.equals("juridica")) {
      nombre = texto(datos, "razonSocial", 255, true);

      donante = new PersonaJuridica(
          email,
          documento,
          TipoDocumento.CUIT,
          nombre,
          opcion(
              TipoOrganizacion.class,
              texto(datos, "tipo", 30, true)
          ),
          texto(datos, "rubro", 255, true)
      );
    } else {
      JsonNode edad = datos.path("edad");

      if (!edad.isIntegralNumber()
          || !edad.canConvertToInt()
          || edad.intValue() < 0
          || edad.intValue() > 120) {
        throw new BadRequestResponse(
            "Ingresá una edad entera entre 0 y 120."
        );
      }

      String nombres = texto(datos, "nombre", 120, true);
      String apellido = texto(datos, "apellido", 120, true);

      nombre = nombres + " " + apellido;

      donante = new PersonaHumana(
          email,
          documento,
          TipoDocumento.DNI,
          nombres,
          apellido,
          edad.intValue(),
          opcion(
              Genero.class,
              texto(datos, "genero", 20, true)
          ),
          texto(datos, "direccion", 255, true)
      );
    }

    donante.activar();

    EntityManager em = proveedor.get();

    validarDuplicados(em, email, documento);

    String issuer = logto.issuer();

    // En Logto guardamos una huella, no el documento.
    String operacion = huella(
        issuer + "\n"
            + email + "\n"
            + clase + "\n"
            + documento
    );

    String sub = logto.crearDonante(
        email,
        nombre,
        operacion
    );

    String cuentaId = issuer + "|" + sub;

    var tx = em.getTransaction();

    try {
      tx.begin();

      /*
       * Coordina altas simultáneas de este endpoint en PostgreSQL,
       * incluso si el deploy tiene varias instancias.
       */
      for (String clave : java.util.stream.Stream.of(
          "email:" + email,
          "doc:" + documento
      ).sorted().toList()) {
        em.createNativeQuery(
                "SELECT 1 FROM "
                    + "pg_advisory_xact_lock(hashtextextended(:clave, 0))"
            )
            .setParameter("clave", clave)
            .getResultList();
      }

      validarDuplicados(em, email, documento);

      if (em.find(CuentaWeb.class, cuentaId) != null
          || em.find(ContactoCuenta.class, cuentaId) != null) {
        throw new ConflictResponse(
            "La cuenta ya tiene un perfil. "
                + "Buscá el donante antes de continuar."
        );
      }

      em.persist(donante);

      CuentaWeb cuenta = new CuentaWeb();
      cuenta.id = cuentaId;
      cuenta.rol = "DONANTE";
      cuenta.nombre = nombre;
      cuenta.donante = donante;

      em.persist(cuenta);

      ContactoCuenta contacto = new ContactoCuenta();
      contacto.id = cuentaId;
      contacto.email = email;
      contacto.pais = pais;
      contacto.telefono = telefono;
      contacto.canal = canal;
      contacto.actualizado = Instant.now();
      contacto.recibeAvisosAdministracion = false;

      em.persist(contacto);

      tx.commit();

    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      em.clear();

      if (e instanceof HttpResponseException) {
        throw e;
      }

      org.slf4j.LoggerFactory.getLogger(getClass()).error(
          "No se pudo guardar el alta de donante "
              + "después de crear la cuenta Logto",
          e
      );

      /*
       * No borramos la cuenta remota: el commit podría haberse
       * confirmado y perdido su respuesta.
       */
      throw new ServiceUnavailableResponse(
          "No se pudo confirmar el guardado local. "
              + "Buscá el correo; si no aparece, "
              + "repetí el alta con los mismos datos."
      );
    }

    ctx.status(201).json(Map.of(
        "id", donante.getId().toString(),
        "nombre", nombre,
        "mail", email,
        "documento", documento
    ));
  }

  private static void validarDuplicados(
      EntityManager em,
      String email,
      String documento
  ) {
    Long cantidad = em.createQuery(
            "SELECT COUNT(d) FROM Donante d "
                + "WHERE LOWER(TRIM(d.mail)) = :email "
                + "OR TRIM(d.numeroDocumento) = :documento",
            Long.class
        )
        .setParameter("email", email)
        .setParameter("documento", documento)
        .getSingleResult();

    if (cantidad > 0) {
      throw new ConflictResponse(
          "Ya existe un donante con ese correo o documento. "
              + "Buscalo para continuar."
      );
    }

    Long contactos = em.createQuery(
            "SELECT COUNT(c) FROM ContactoCuenta c "
                + "WHERE LOWER(TRIM(c.email)) = :email",
            Long.class
        )
        .setParameter("email", email)
        .getSingleResult();

    if (contactos > 0) {
      throw new ConflictResponse(
          "Ese correo ya está vinculado a un perfil de DonaTrack."
      );
    }
  }

  private static String texto(
      JsonNode datos,
      String campo,
      int maximo,
      boolean obligatorio
  ) {
    JsonNode valor = datos.path(campo);

    if (!valor.isMissingNode()
        && !valor.isNull()
        && !valor.isTextual()) {
      throw new BadRequestResponse(
          "Revisá el campo " + campo + "."
      );
    }

    String texto = valor.isTextual()
        ? valor.asText().trim()
        : "";

    if (texto.length() > maximo
        || obligatorio && texto.isBlank()) {
      throw new BadRequestResponse(
          "Completá correctamente el campo " + campo + "."
      );
    }

    return texto;
  }

  private static <T extends Enum<T>> T opcion(
      Class<T> tipo,
      String valor
  ) {
    try {
      return Enum.valueOf(tipo, valor);
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse(
          "Elegí una opción válida."
      );
    }
  }

  private static String huella(String datos) {
    try {
      return HexFormat.of().formatHex(
          MessageDigest.getInstance("SHA-256")
              .digest(datos.getBytes(StandardCharsets.UTF_8))
      );
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}