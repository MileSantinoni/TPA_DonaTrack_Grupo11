package org.example.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.Issuer;
import com.nimbusds.openid.connect.sdk.Nonce;
import com.nimbusds.openid.connect.sdk.validators.IDTokenValidator;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import java.io.Serializable;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class LogtoController {

  private static final String PENDIENTE = "logto.pendiente";
  private static final String USUARIO = "logto.usuario";
  private static final String ID_TOKEN = "logto.idToken";
  private static final String CSRF = "logto.csrf";

  private static final SecureRandom RANDOM = new SecureRandom();

  private final ObjectMapper json = new ObjectMapper();

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();

  public record Usuario(
      String issuer,
      String sub,
      String nombre,
      String email,
      boolean emailVerificado,
      List<String> roles,
      Instant vence
  ) implements Serializable {
  }

  private record Intento(
      String state,
      String nonce,
      String verifier,
      Instant creado,
      String tipo
  ) implements Serializable {
  }

  public void login(Context ctx) throws Exception {
    iniciar(ctx, false);
  }

  public void registro(Context ctx) throws Exception {
    String tipo = ctx.queryParam("tipo");

    var existente = org.example.seguridad.SesionWeb.opcional(ctx);

    if (existente != null
        && (org.example.seguridad.SesionWeb.esAdmin(existente)
        || ctx.attribute("cuentaWeb") != null)) {

      ctx.redirect("/cuenta/continuar");
      return;
    }

    if (tipo == null) {
      ctx.header("Cache-Control", "no-store");

      ctx.html("""
            <!doctype html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport"
                      content="width=device-width,initial-scale=1">
                <title>Registro · DonaTrack</title>
                <link rel="stylesheet" href="/assets/main.css">
            </head>
            <body>
                <main style="max-width:600px;margin:80px auto;padding:24px">
                    <h1>Registrate en DonaTrack</h1>
                    <p>
                        Elegí cómo vas a participar.
                        Esta elección queda asociada a tu cuenta.
                    </p>
                    <p>
                        <a class="btn btn-primary-action"
                           href="/auth/registro?tipo=DONANTE">
                            Quiero donar
                        </a>
                    </p>
                    <p>
                        <a class="btn btn-secondary"
                           href="/auth/registro?tipo=ENTIDAD">
                            Registrar mi entidad
                        </a>
                    </p>
                    <p>
                        <a href="/logout">Cerrar sesión</a>
                        ·
                        <a href="/login">Iniciar sesión</a>
                    </p>
                </main>
            </body>
            </html>
            """);

      return;
    }

    if (!List.of("DONANTE", "ENTIDAD").contains(tipo)) {
      throw new BadRequestResponse(
          "Tipo de registro inválido."
      );
    }

    var usuario = org.example.seguridad.SesionWeb.opcional(ctx);

    if (usuario != null) {
      org.example.seguridad.SesionWeb.registrarTipo(
          ctx,
          usuario,
          tipo
      );

      ctx.redirect("/cuenta/continuar");
      return;
    }

    iniciar(ctx, true);
  }

  private void iniciar(Context ctx, boolean registro)
      throws Exception {

    if (org.example.seguridad.SesionWeb.opcional(ctx) != null) {
      ctx.redirect("/cuenta/continuar");
      return;
    }

    String issuer = issuer();
    String appId = variable("LOGTO_APP_ID");
    String redirect = variable("LOGTO_REDIRECT_URI");

    variable("LOGTO_APP_SECRET");

    var session = ctx.req().getSession(true);
    session.setMaxInactiveInterval(30 * 60);

    Intento intento = new Intento(
        aleatorio(),
        aleatorio(),
        aleatorio(),
        Instant.now(),
        registro ? ctx.queryParam("tipo") : null
    );

    synchronized (session) {
      var pendientes = intentos(session);

      pendientes.values().removeIf(i ->
          i.creado().plusSeconds(600).isBefore(Instant.now())
      );

      if (pendientes.size() >= 5) {
        pendientes.remove(
            pendientes.keySet().iterator().next()
        );
      }

      pendientes.put(intento.state(), intento);
      session.setAttribute(PENDIENTE, pendientes);
    }

    String challenge = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(
            MessageDigest.getInstance("SHA-256").digest(
                intento.verifier()
                    .getBytes(StandardCharsets.US_ASCII)
            )
        );

    Map<String, String> parametros = new LinkedHashMap<>();

    parametros.put("client_id", appId);
    parametros.put("redirect_uri", redirect);
    parametros.put("response_type", "code");
    parametros.put("scope", "openid profile email roles");
    parametros.put("state", intento.state());
    parametros.put("nonce", intento.nonce());
    parametros.put("code_challenge", challenge);
    parametros.put("code_challenge_method", "S256");
    parametros.put(
        "first_screen",
        registro ? "register" : "sign_in"
    );

    ctx.header("Cache-Control", "no-store");
    ctx.redirect(issuer + "/auth?" + formulario(parametros));
  }

  public void callback(Context ctx) {
    ctx.header("Cache-Control", "no-store");
    ctx.header("Referrer-Policy", "no-referrer");

    var session = ctx.req().getSession(false);

    if (session == null) {
      throw new BadRequestResponse(
          "El ingreso venció. Volvé a iniciar sesión."
      );
    }

    if (org.example.seguridad.SesionWeb.opcional(ctx) != null) {
      ctx.redirect("/cuenta/continuar");
      return;
    }

    Intento intento;

    synchronized (session) {
      var pendientes = intentos(session);
      intento = pendientes.remove(ctx.queryParam("state"));
      session.setAttribute(PENDIENTE, pendientes);
    }

    if (intento == null
        || intento.creado().plusSeconds(600).isBefore(Instant.now())
        || !iguales(intento.state(), ctx.queryParam("state"))) {

      throw new BadRequestResponse(
          "El ingreso venció o ya se utilizó. Volvé a /login."
      );
    }

    if (ctx.queryParam("error") != null) {
      throw new BadRequestResponse(
          "No se completó el ingreso en Logto."
      );
    }

    String code = ctx.queryParam("code");

    if (code == null || code.isBlank()) {
      throw new BadRequestResponse(
          "Logto no devolvió el código de autorización."
      );
    }

    try {
      String issuer = issuer();
      String issuerRespuesta = ctx.queryParam("iss");

      if (issuerRespuesta != null
          && !issuer.equals(issuerRespuesta)) {
        throw new IllegalArgumentException(
            "Emisor inesperado"
        );
      }

      String cuerpo = formulario(Map.of(
          "grant_type", "authorization_code",
          "client_id", variable("LOGTO_APP_ID"),
          "client_secret", variable("LOGTO_APP_SECRET"),
          "redirect_uri", variable("LOGTO_REDIRECT_URI"),
          "code", code,
          "code_verifier", intento.verifier()
      ));

      var request = HttpRequest.newBuilder(
              URI.create(issuer + "/token")
          )
          .timeout(Duration.ofSeconds(20))
          .header(
              "Content-Type",
              "application/x-www-form-urlencoded"
          )
          .header("Accept", "application/json")
          .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
          .build();

      var response = http.send(
          request,
          HttpResponse.BodyHandlers.ofString(
              StandardCharsets.UTF_8
          )
      );

      if (response.statusCode() != 200) {
        throw new IllegalStateException(
            "Logto rechazó el intercambio del código"
        );
      }

      String idToken = json.readTree(response.body())
          .path("id_token")
          .asText("");

      SignedJWT jwt = SignedJWT.parse(idToken);

      // Conserva el algoritmo configurado en la integración actual.
      var validator = new IDTokenValidator(
          new Issuer(issuer),
          new ClientID(variable("LOGTO_APP_ID")),
          JWSAlgorithm.ES384,
          URI.create(issuer + "/jwks").toURL()
      );

      validator.validate(jwt, new Nonce(intento.nonce()));

      var claims = jwt.getJWTClaimsSet();
      String sub = claims.getSubject();

      if (sub == null || sub.isBlank()) {
        throw new IllegalArgumentException(
            "Falta el identificador"
        );
      }

      List<String> roles = claims.getStringListClaim("roles");

      Instant vence = claims.getExpirationTime().toInstant();
      Instant limiteLocal = Instant.now().plusSeconds(3600);

      if (vence.isAfter(limiteLocal)) {
        vence = limiteLocal;
      }

      if (!vence.isAfter(Instant.now())) {
        throw new IllegalArgumentException("Token vencido");
      }

      var usuario = new Usuario(
          issuer,
          sub,
          Objects.toString(
              claims.getStringClaim("name"),
              ""
          ),
          Objects.toString(
              claims.getStringClaim("email"),
              ""
          ),
          Boolean.TRUE.equals(
              claims.getBooleanClaim("email_verified")
          ),
          roles == null ? List.of() : List.copyOf(roles),
          vence
      );

      synchronized (session) {
        if (session.getAttribute(USUARIO) == null) {
          org.example.seguridad.SesionWeb.registrarTipo(
              ctx,
              usuario,
              intento.tipo()
          );

          ctx.req().changeSessionId();

          session.setAttribute(USUARIO, usuario);
          session.setAttribute(ID_TOKEN, idToken);
          session.setAttribute(CSRF, aleatorio());
          session.removeAttribute(PENDIENTE);
        }
      }

      ctx.redirect("/cuenta/continuar");

    } catch (Exception e) {
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }

      // No registra tokens, códigos ni secretos.
      org.slf4j.LoggerFactory
          .getLogger(LogtoController.class)
          .warn(
              "Fallo de callback Logto, tipo={}",
              e.getClass().getSimpleName()
          );

      ctx.status(401).result(
          "No se pudo validar el ingreso. "
              + "Volvé a entrar desde /login."
      );
    }
  }

  public void actual(Context ctx) {
    Usuario usuario = usuario(ctx);

    org.example.seguridad.CuentaWeb cuenta =
        ctx.attribute("cuentaWeb");

    boolean admin =
        org.example.seguridad.SesionWeb.esAdmin(usuario);

    boolean completa =
        admin || cuenta != null && cuenta.completa();

    List<String> roles = admin
        ? List.of("ADMIN")
        : cuenta != null && cuenta.completa()
          ? List.of(cuenta.rol)
          : List.of();

    ctx.header("Cache-Control", "no-store");

    ctx.json(Map.of(
        "autenticado", true,
        "sub", usuario.sub(),
        "nombre", cuenta == null ? usuario.nombre() : cuenta.nombre,
        "email", usuario.email(),
        "emailVerificado", usuario.emailVerificado(),
        "perfilCompleto", completa,
        "roles", roles
    ));
  }

  public static Usuario usuario(Context ctx) {
    var session = ctx.req().getSession(false);

    Usuario usuario = session == null
        ? null
        : (Usuario) session.getAttribute(USUARIO);

    if (usuario == null) {
      throw new UnauthorizedResponse("Iniciá sesión para continuar.");
    }

    if (!usuario.vence().isAfter(Instant.now())) {
      session.invalidate();
      throw new UnauthorizedResponse(
          "La sesión venció. Volvé a iniciar sesión."
      );
    }

    return usuario;
  }

  public void confirmarSalida(Context ctx) {
    usuario(ctx);

    String csrf = (String) ctx.req()
        .getSession(false)
        .getAttribute(CSRF);

    ctx.header("Cache-Control", "no-store");

    ctx.html("""
        <!doctype html>
        <html lang="es">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>Cerrar sesión · DonaTrack</title>
          <link rel="stylesheet" href="/assets/main.css">
        </head>
        <body>
          <main style="max-width:480px;margin:60px auto;padding:24px">
            <h1>Cerrar sesión</h1>
            <p>¿Querés cerrar tu sesión de DonaTrack?</p>
            <form method="post" action="/auth/logout">
              <input type="hidden" name="csrf" value="%s">
              <button class="btn btn-primary-action" type="submit">
                Cerrar sesión
              </button>
              <a class="btn btn-secondary" href="/">Cancelar</a>
            </form>
          </main>
        </body>
        </html>
        """.formatted(csrf));
  }

  public void logout(Context ctx) {
    var session = ctx.req().getSession(false);

    if (session == null) {
      ctx.redirect("/");
      return;
    }

    String esperado = (String) session.getAttribute(CSRF);

    if (!iguales(esperado, ctx.formParam("csrf"))) {
      throw new ForbiddenResponse("Solicitud de cierre inválida.");
    }

    String idToken = (String) session.getAttribute(ID_TOKEN);

    String destino = issuer() + "/session/end?"
        + formulario(Map.of(
        "id_token_hint", Objects.toString(idToken, ""),
        "post_logout_redirect_uri",
        variable("LOGTO_POST_LOGOUT_REDIRECT_URI")
    ));

    session.invalidate();

    ctx.header("Cache-Control", "no-store");
    ctx.redirect(destino);
  }

  private static String issuer() {
    String valor = variable("LOGTO_ISSUER").replaceAll("/+$", "");

    if (!"https".equalsIgnoreCase(URI.create(valor).getScheme())) {
      throw new IllegalStateException("LOGTO_ISSUER debe usar HTTPS.");
    }

    return valor;
  }

  private static String variable(String nombre) {
    String valor = System.getenv(nombre);

    if (valor == null || valor.isBlank()) {
      throw new IllegalStateException("Falta configurar " + nombre);
    }

    return valor.trim();
  }

  private static String aleatorio() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);

    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(bytes);
  }

  private static boolean iguales(String esperado, String recibido) {
    return esperado != null
        && recibido != null
        && MessageDigest.isEqual(
        esperado.getBytes(StandardCharsets.UTF_8),
        recibido.getBytes(StandardCharsets.UTF_8)
    );
  }

  private static String formulario(Map<String, String> valores) {
    return valores.entrySet().stream()
        .map(e -> codificar(e.getKey()) + "=" + codificar(e.getValue()))
        .collect(Collectors.joining("&"));
  }

  private static String codificar(String valor) {
    return URLEncoder.encode(valor, StandardCharsets.UTF_8);
  }

  public static String csrf(Context ctx) {
    usuario(ctx);

    return (String) ctx.req()
        .getSession(false)
        .getAttribute(CSRF);
  }

  public static void validarCsrf(Context ctx) {
    if (!iguales(csrf(ctx), ctx.formParam("csrf"))) {
      throw new ForbiddenResponse(
          "El formulario venció. Recargá la página."
      );
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Intento> intentos(
      jakarta.servlet.http.HttpSession session
  ) {
    Object valor = session.getAttribute(PENDIENTE);

    return valor instanceof Map<?, ?>
        ? new LinkedHashMap<>((Map<String, Intento>) valor)
        : new LinkedHashMap<>();
  }
}