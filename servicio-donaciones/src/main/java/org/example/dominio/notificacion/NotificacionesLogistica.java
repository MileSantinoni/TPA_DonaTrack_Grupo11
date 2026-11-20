package org.example.dominio.notificacion;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.*;

import javax.persistence.EntityManager;

import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.EventoLogistico;
import org.example.seguridad.CuentaWeb;

public final class NotificacionesLogistica {

  private static final DateTimeFormatter FECHA =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

  private NotificacionesLogistica() {}

  public static void validarConfiguracion() {
    origenPublico();
  }

  public static List<NotificacionPendiente> preparar(
      EntityManager em,
      AsignacionDonacion asignacion,
      EventoLogistico evento
  ) {
    if (evento.tipo() == EventoLogistico.Tipo.RETORNO_DEPOSITO) {
      return List.of();
    }

    var donacion = asignacion.getDonacion();
    var entidad = asignacion.getEntidad();

    String id = donacion.getIdAsString();

    String datos = "Donación: «"
        + Objects.toString(
        donacion.getDescripcionGeneral(),
        "Donación"
    )
        + "».\nEntidad: " + entidad.getRazonSocial()
        + ".\nCamión: " + evento.patente()
        + ".\nFecha y hora: "
        + evento.fecha().format(FECHA)
        + ".";

    String titulo;
    String mensaje;

    switch (evento.tipo()) {
      case INICIO_TRASLADO -> {
        titulo = "La entrega está en camino";

        mensaje = datos
            + "\nEl chofer inició la ruta. "
            + "Podés seguir esta entrega en el mapa interactivo.";
      }

      case RECEPCION -> {
        titulo = "Entrega confirmada";

        mensaje = "Comprobante de entrega\n"
            + datos
            + "\nLa entidad confirmó la recepción satisfactoria."
            + "\nDonación: " + id
            + "\nComprobante: " + evento.idOperacion();
      }

      case NO_RECIBIDA -> {
        titulo = "No se pudo completar la entrega";

        mensaje = datos
            + "\nMotivo: " + evento.motivo()
            + "\nLa donación quedó en estado Entrega fallida. "
            + "Administración debe evaluar si corresponde replanificarla.";
      }

      default -> throw new IllegalArgumentException(
          "Evento no soportado"
      );
    }

    List<NotificacionPendiente> pendientes = new ArrayList<>();

    CuentaWeb cuentaEntidad = buscarCuenta(
        em,
        "entidad",
        entidad.getId()
    );

    pendientes.add(prepararPara(
        em,
        cuentaEntidad == null ? null : cuentaEntidad.id,
        "ENTIDAD:" + entidad.getId(),
        evento,
        titulo,
        mensaje,
        "/mi-entidad?entrega=" + id
    ));

    var donante = donacion.getDonante();

    if (donante != null) {
      CuentaWeb cuentaDonante = buscarCuenta(
          em,
          "donante",
          donante.getId()
      );

      pendientes.add(prepararPara(
          em,
          cuentaDonante == null ? null : cuentaDonante.id,
          "DONANTE:" + donante.getId(),
          evento,
          titulo,
          mensaje,
          "/mis-donaciones?entrega=" + id
      ));
    }

    if (evento.tipo() == EventoLogistico.Tipo.NO_RECIBIDA) {
      for (String cuentaId : administradores(em)) {
        String referencia = "ADMIN:"
            + UUID.nameUUIDFromBytes(
            cuentaId.getBytes(StandardCharsets.UTF_8)
        );

        pendientes.add(prepararPara(
            em,
            cuentaId,
            referencia,
            evento,
            titulo,
            mensaje,
            "/administracion/donaciones"
        ));
      }
    }

    return pendientes;
  }

  private static CuentaWeb buscarCuenta(
      EntityManager em,
      String relacion,
      Object id
  ) {
    return em.createQuery(
            "SELECT c FROM CuentaWeb c "
                + "WHERE c." + relacion + ".id = :id",
            CuentaWeb.class
        )
        .setParameter("id", id)
        .getResultStream()
        .findFirst()
        .orElse(null);
  }

  private static NotificacionPendiente prepararPara(
      EntityManager em,
      String cuentaId,
      String referencia,
      EventoLogistico evento,
      String titulo,
      String mensaje,
      String ruta
  ) {
    var pendiente = NotificacionesCuenta.preparar(
        em,
        cuentaId,
        referencia,
        evento.idOperacion(),
        titulo,
        mensaje
    );

    pendiente.vincularAccion(ruta);
    return pendiente;
  }

  public static String urlPublica(String ruta) {
    return origenPublico() + ruta;
  }

  private static String origenPublico() {
    String base = Objects.toString(
        System.getenv("PUBLIC_BASE_URL"),
        ""
    ).trim();

    boolean explicita = !base.isEmpty();

    if (!explicita) {
      base = Objects.toString(
          System.getenv("LOGTO_REDIRECT_URI"),
          ""
      ).trim();
    }

    try {
      URI uri = URI.create(base);

      if (!Set.of("https", "http").contains(
          Objects.toString(uri.getScheme(), "")
      )
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getQuery() != null
          || uri.getFragment() != null
          || (explicita && !Set.of("", "/").contains(uri.getPath()))) {
        throw new IllegalArgumentException();
      }

      return uri.getScheme() + "://" + uri.getRawAuthority();

    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "Configurá PUBLIC_BASE_URL con el origen público "
              + "de Donaciones o una LOGTO_REDIRECT_URI válida.",
          e
      );
    }
  }

  private static List<String> administradores(EntityManager em) {
    return em.createQuery(
            "SELECT c.id FROM ContactoCuenta c "
                + "WHERE c.recibeAvisosAdministracion = true "
                + "ORDER BY c.id",
            String.class
        )
        .getResultList();
  }
}