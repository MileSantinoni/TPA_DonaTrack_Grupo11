package org.example.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.http.*;
import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import javax.persistence.*;
import org.example.dominio.beneficiario.*;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.donacion.*;

public class MiEntidadController {

  private final Supplier<EntityManager> proveedor;

  final ObjectMapper json = new ObjectMapper();

  private final HttpClient http = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build();

  private final String logistica = System.getenv()
      .getOrDefault("LOGISTICA_URL", "http://localhost:8082")
      .replaceAll("/+$", "");

  public MiEntidadController(Supplier<EntityManager> proveedor) {
    this.proveedor = proveedor;
  }

  EntidadBeneficiaria entidadActual(Context ctx) {
    EntidadBeneficiaria entidad =
        org.example.seguridad.SesionWeb.cuenta(ctx).entidad;

    if (entidad == null) {
      throw new io.javalin.http.ForbiddenResponse(
          "La cuenta no tiene una entidad vinculada."
      );
    }

    return entidad;
  }

  public void index(Context ctx) {
    var em = proveedor.get();
    var entidad = entidadActual(ctx);

    boolean necesidades =
        "necesidades".equals(ctx.queryParam("vista"));

    String q = texto(ctx.queryParam("q"));
    String estado = texto(ctx.queryParam("estado"));
    String tipo = texto(ctx.queryParam("tipo"));
    String cobertura = texto(ctx.queryParam("cobertura"));

    int pagina = 1;

    try {
      if (ctx.queryParam("pagina") != null) {
        pagina = Integer.parseInt(ctx.queryParam("pagina"));
      }

      if (pagina < 1 || pagina > 100000) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException e) {
      throw new BadRequestResponse("Página inválida.");
    }

    Map<String, Object> modelo = new HashMap<>();
    Map<String, Object> params = new HashMap<>();

    params.put("entidad", entidad.getId());

    String from;

    if (necesidades) {
      from = """
           FROM Necesidad n
           WHERE n.entidadBeneficiaria.id = :entidad
          """;

      if (!tipo.isEmpty()) {
        if (!List.of("Recurrente", "Extraordinaria").contains(tipo)) {
          throw new BadRequestResponse("Tipo inválido.");
        }

        from += " AND TYPE(n) = "
            + (tipo.equals("Recurrente")
            ? "NecesidadRecurrente"
            : "NecesidadExtraordinaria");
      }

      if (!cobertura.isEmpty()) {
        if (!List.of("pendiente", "cubierta").contains(cobertura)) {
          throw new BadRequestResponse("Cobertura inválida.");
        }
      }

      if (!q.isEmpty()) {
        from += " AND LOWER(n.descripcion) LIKE :q ESCAPE '!'";
      }
    } else {
      from = """
           FROM Donacion d
           WHERE EXISTS (
             SELECT a.id FROM AsignacionDonacion a
             WHERE a.donacion = d
               AND a.entidad.id = :entidad
           )
          """;

      if (!estado.isEmpty()) {
        try {
          EstadoDonacion.valueOf(estado);
        } catch (IllegalArgumentException e) {
          throw new BadRequestResponse("Estado inválido.");
        }

        from += " AND d.estadoActualTexto = :estado";
        params.put("estado", estado);
      }

      if (!q.isEmpty()) {
        from += """
             AND (
               LOWER(d.descripcionGeneral) LIKE :q ESCAPE '!'
               OR LOWER(str(d.id)) LIKE :q ESCAPE '!'
             )
            """;
      }
    }

    if (!q.isEmpty()) {
      params.put(
          "q",
          "%" + q.toLowerCase(Locale.ROOT)
              .replace("!", "!!")
              .replace("%", "!%")
              .replace("_", "!_") + "%"
      );
    }

    long total;
    long paginas;

    List<Map<String, Object>> filas = new ArrayList<>();

    if (necesidades) {
      var consulta = em.createQuery(
          "SELECT n" + from + " ORDER BY n.id DESC",
          Necesidad.class
      );

      params.forEach(consulta::setParameter);

      LocalDate hoy = LocalDate.now();

      List<Necesidad> lista = consulta.getResultList().stream()
          .filter(n ->
              cobertura.isEmpty()
                  || (n.cantidadCubiertaEn(hoy)
                  >= n.getCantidadObjetivo())
                  == cobertura.equals("cubierta")
          )
          .toList();

      total = lista.size();
      paginas = Math.max(1, (total + 9) / 10);
      pagina = (int) Math.min(pagina, paginas);

      lista.stream()
          .skip((pagina - 1L) * 10)
          .limit(10)
          .map(this::datosNecesidad)
          .forEach(filas::add);

    } else {
      var cuenta = em.createQuery(
          "SELECT COUNT(d)" + from,
          Long.class
      );

      params.forEach(cuenta::setParameter);

      total = cuenta.getSingleResult();
      paginas = Math.max(1, (total + 9) / 10);
      pagina = (int) Math.min(pagina, paginas);

      var consulta = em.createQuery(
          "SELECT d" + from
              + " ORDER BY d.fechaDeRegistro DESC, d.id",
          Donacion.class
      );

      params.forEach(consulta::setParameter);

      consulta.setFirstResult((pagina - 1) * 10)
          .setMaxResults(10)
          .getResultList()
          .stream()
          .map(this::datosDonacion)
          .forEach(filas::add);
    }

    modelo.put(
        necesidades ? "necesidades" : "donaciones",
        filas
    );

    modelo.put("esNecesidades", necesidades);

    String nombreEntidad = texto(entidad.getRazonSocial());

    if (nombreEntidad.isEmpty()) {
      nombreEntidad = "Entidad";
    }

    modelo.put("entidadNombre", nombreEntidad);
    modelo.put(
        "entidadInicial",
        nombreEntidad.substring(0, 1).toUpperCase(Locale.ROOT)
    );

    modelo.put("q", q);
    modelo.put("total", total);
    modelo.put("pagina", pagina);
    modelo.put("paginas", paginas);
    modelo.put("hayPaginacion", paginas > 1);

    String enlace = "/mi-entidad?vista="
        + (necesidades ? "necesidades" : "donaciones")
        + "&q=" + codificar(q)
        + "&tipo=" + codificar(tipo)
        + "&cobertura=" + codificar(cobertura)
        + "&estado=" + codificar(estado)
        + "&pagina=";

    if (pagina > 1) {
      modelo.put("anterior", enlace + (pagina - 1));
    }

    if (pagina < paginas) {
      modelo.put("siguiente", enlace + (pagina + 1));
    }

    modelo.put(
        "estados",
        Arrays.stream(EstadoDonacion.values())
            .map(e -> Map.of(
                "valor", e.name(),
                "nombre", etiqueta(e),
                "elegido", e.name().equals(estado)
            ))
            .toList()
    );

    modelo.put(
        "tipos",
        opciones(List.of("Recurrente", "Extraordinaria"), tipo)
    );

    modelo.put("coberturaPendiente", cobertura.equals("pendiente"));
    modelo.put("coberturaCubierta", cobertura.equals("cubierta"));

    modelo.put(
        "catalogo",
        em.createQuery(
                "SELECT s FROM Subcategoria s ORDER BY s.nombre",
                Subcategoria.class
            )
            .getResultList()
            .stream()
            .map(s -> Map.of(
                "id", s.getId(),
                "nombre", s.getNombre(),
                "categoria",
                s.getCategoria() == null
                    ? "Sin categoría"
                    : s.getCategoria().getNombre()
            ))
            .toList()
    );

    modelo.put(
        "googleMapsKey",
        System.getenv().getOrDefault("GOOGLE_MAPS_API_KEY", "")
    );

    ctx.header("Cache-Control", "no-store")
        .render("miEntidad.hbs", modelo);
  }

  public void detalleNecesidad(Context ctx) {
    ctx.json(datosNecesidad(necesidadPropia(ctx)));
  }

  public void guardarNecesidad(Context ctx) {
    var entidad = entidadActual(ctx);
    var em = proveedor.get();

    String tipo = campo(ctx, "tipo");
    String descripcion = campo(ctx, "descripcion");

    if (!List.of("Recurrente", "Extraordinaria").contains(tipo)
        || descripcion.isEmpty()
        || descripcion.length() > 180) {
      throw new BadRequestResponse(
          "Revisá el tipo y la descripción."
      );
    }

    String unidad;

    try {
      unidad = Necesidad.normalizarUnidad(
          campo(ctx, "unidad")
      );
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse(e.getMessage());
    }

    int objetivo;

    try {
      objetivo = Integer.parseInt(campo(ctx, "objetivo"));

      if (objetivo <= 0) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException e) {
      throw new BadRequestResponse(
          "La cantidad debe ser un entero positivo."
      );
    }

    Subcategoria sub = em.find(
        Subcategoria.class,
        campo(ctx, "subcategoria")
    );

    if (sub == null) {
      throw new BadRequestResponse(
          "Seleccioná una subcategoría existente."
      );
    }

    LocalDate inicio = null;
    Periodicidad periodo = null;
    String motivo = campo(ctx, "motivo");

    if (tipo.equals("Recurrente")) {
      try {
        inicio = LocalDate.parse(campo(ctx, "inicio"));
        periodo = Periodicidad.valueOf(campo(ctx, "periodicidad"));
      } catch (IllegalArgumentException
               | java.time.format.DateTimeParseException e) {
        throw new BadRequestResponse(
            "Revisá fecha y periodicidad."
        );
      }
    } else if (motivo.isEmpty() || motivo.length() > 240) {
      throw new BadRequestResponse(
          "Indicá un motivo de hasta 240 caracteres."
      );
    }

    var tx = em.getTransaction();
    tx.begin();

    try {
      Necesidad n;

      if (ctx.pathParamMap().containsKey("id")) {
        n = necesidadPropia(ctx);
        em.refresh(n, LockModeType.PESSIMISTIC_WRITE);

        if (tipo.equals("Recurrente")
            != (n instanceof NecesidadRecurrente)) {
          throw new ConflictResponse(
              "No se puede cambiar el tipo de una necesidad registrada."
          );
        }

        boolean vinculada = vinculada(n) || n.tieneAportes();

        if (vinculada
            && !Objects.equals(n.getUnidadMedida(), unidad)) {
          throw new ConflictResponse(
              "No se puede cambiar la unidad de una necesidad "
                  + "con asignaciones. Creá una nueva necesidad "
                  + "para esa unidad."
          );
        }

        String subActual = n.getSubcategoria() == null
            ? null
            : n.getSubcategoria().getId();

        if (vinculada && !Objects.equals(sub.getId(), subActual)) {
          throw new ConflictResponse(
              "La subcategoría no puede cambiar porque tiene asignaciones."
          );
        }

        if (vinculada
            && n instanceof NecesidadRecurrente r
            && (!Objects.equals(inicio, r.getFechaInicioPeriodo())
            || periodo != r.getPeriodicidad())) {
          throw new ConflictResponse(
              "El período no puede cambiar porque tiene asignaciones."
          );
        }

        n.actualizarDatos(descripcion, objetivo, sub);

        if (n instanceof NecesidadRecurrente r) {
          r.actualizarPeriodo(inicio, periodo);
        } else {
          ((NecesidadExtraordinaria) n).actualizarMotivo(motivo);
        }
      } else {
        n = tipo.equals("Recurrente")
            ? new NecesidadRecurrente(
            descripcion, objetivo, sub, inicio, periodo
        )
            : new NecesidadExtraordinaria(
            descripcion, objetivo, sub, motivo
        );

        entidad.registrarNecesidad(n);
        em.persist(n);
      }

      n.actualizarUnidad(unidad);
      tx.commit();

      ctx.json(Map.of("mensaje", "Necesidad guardada."));
    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
    }
  }

  public void eliminarNecesidad(Context ctx) {
    var em = proveedor.get();
    var tx = em.getTransaction();

    tx.begin();

    try {
      Necesidad n = necesidadPropia(ctx);
      em.refresh(n, LockModeType.PESSIMISTIC_WRITE);

      if (vinculada(n) || n.tieneAportes()) {
        throw new ConflictResponse(
            "No se puede eliminar una necesidad "
                + "con asignaciones o cobertura registrada."
        );
      }

      n.getEntidadBeneficiaria().getNecesidades().remove(n);
      em.remove(n);

      tx.commit();

      ctx.json(Map.of("mensaje", "Necesidad eliminada."));
    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
    }
  }

  private Necesidad necesidadPropia(Context ctx) {
    long id;

    try {
      id = Long.parseLong(ctx.pathParam("id"));
    } catch (NumberFormatException e) {
      throw new BadRequestResponse("ID inválido.");
    }

    var entidad = entidadActual(ctx);
    var n = proveedor.get().find(Necesidad.class, id);

    if (n == null
        || !entidad.getId().equals(
        n.getEntidadBeneficiaria().getId()
    )) {
      throw new NotFoundResponse(
          "No existe esa necesidad en tu entidad."
      );
    }

    return n;
  }

  private boolean vinculada(Necesidad n) {
    return proveedor.get().createQuery(
            "SELECT COUNT(a) FROM AsignacionDonacion a "
                + "WHERE a.necesidad = :n",
            Long.class
        )
        .setParameter("n", n)
        .getSingleResult() > 0;
  }

  Donacion donacionPropia(Context ctx, String idTexto) {
    UUID id;

    try {
      id = UUID.fromString(idTexto);
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("ID inválido.");
    }

    var entidad = entidadActual(ctx);

    var lista = proveedor.get().createQuery("""
            SELECT DISTINCT a.donacion
            FROM AsignacionDonacion a
            WHERE a.entidad.id = :entidad
              AND a.donacion.id = :id
            """, Donacion.class)
        .setParameter("entidad", entidad.getId())
        .setParameter("id", id)
        .getResultList();

    if (lista.isEmpty()) {
      throw new NotFoundResponse(
          "No existe esa donación en tu entidad."
      );
    }

    return lista.get(0);
  }

  public void seguimiento(Context ctx) {
    ctx.header("Cache-Control", "no-store")
        .json(seguimientoDatos(ctx, ctx.pathParam("id")));
  }

  ObjectNode seguimientoDatos(Context ctx, String id) {
    var d = donacionPropia(ctx, id);
    var entidad = entidadActual(ctx);

    ObjectNode datos = llamarLogistica(
        "GET",
        "/interno/donaciones/" + d.getIdAsString() + "/seguimiento",
        null
    );

    if (!entidad.getIdAsString().equals(
        datos.path("idEntidad").asText()
    )) {
      throw new ConflictResponse(
          "El destino de la ruta no coincide con tu entidad."
      );
    }

    if (entidad.getLatitud() != null
        && entidad.getLongitud() != null) {
      datos.putObject("destino")
          .put("lat", entidad.getLatitud())
          .put("lng", entidad.getLongitud());
    }

    return datos;
  }

  ObjectNode llamarLogistica(
      String metodo,
      String ruta,
      Object cuerpo
  ) {
    try {
      var b = HttpRequest.newBuilder(
              URI.create(logistica + ruta)
          )
          .timeout(Duration.ofSeconds(20))
          .header("Accept", "application/json");

      if (cuerpo == null) {
        b.GET();
      } else {
        b.header("Content-Type", "application/json")
            .method(
                metodo,
                HttpRequest.BodyPublishers.ofString(
                    json.writeValueAsString(cuerpo)
                )
            );
      }

      var r = http.send(
          b.build(),
          HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
      );

      if (r.statusCode() == 404) {
        throw new NotFoundResponse(
            "No hay una ruta registrada para esta entrega."
        );
      }

      if (r.statusCode() == 400 || r.statusCode() == 409) {
        throw new ConflictResponse(
            "Logística rechazó la operación. "
                + "Actualizá la página y verificá el estado de la entrega."
        );
      }

      if (r.statusCode() < 200 || r.statusCode() >= 300) {
        throw new BadGatewayResponse(
            "Logística no pudo completar la operación."
        );
      }

      if (r.statusCode() == 204) {
        return json.createObjectNode();
      }

      var nodo = json.readTree(r.body());

      if (!(nodo instanceof ObjectNode objeto)) {
        throw new IOException("Respuesta inválida.");
      }

      return objeto;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      throw new ServiceUnavailableResponse(
          "Comunicación interrumpida. Volvé a consultar el estado."
      );
    } catch (IOException e) {
      throw new BadGatewayResponse(
          "No se pudo confirmar la operación con Logística. "
              + "Podés volver a consultar o reintentar."
      );
    }
  }

  private Map<String, Object> datosNecesidad(Necesidad n) {
    Map<String, Object> m = new HashMap<>();

    m.put("id", n.getId().toString());
    m.put("descripcion", n.getDescripcion());
    m.put("objetivo", n.getCantidadObjetivo());
    m.put("cubierta", n.getCantidadCubierta());
    m.put("pendiente", Math.max(0, n.getCantidadPendiente()));
    m.put("unidad", Objects.toString(n.getUnidadMedida(), ""));
    m.put("unidadTexto", Objects.toString(n.getUnidadMedida(), "Unidad pendiente"));

    var sub = n.getSubcategoria();

    m.put("subcategoria", sub == null ? "" : sub.getId());
    m.put(
        "subcategoriaNombre",
        sub == null ? "Sin subcategoría" : sub.getNombre()
    );

    m.put(
        "categoria",
        sub == null || sub.getCategoria() == null
            ? "Sin categoría"
            : sub.getCategoria().getNombre()
    );

    boolean vinculada = vinculada(n) || n.tieneAportes();

    m.put("vinculada", vinculada);
    m.put("eliminable", !vinculada && !n.tieneAportes());

    if (n instanceof NecesidadRecurrente r) {
      m.put("tipo", "Recurrente");
      m.put("recurrente", true);
      m.put("periodicidad", Objects.toString(r.getPeriodicidad(), ""));
      m.put("inicio", Objects.toString(r.getFechaInicioPeriodo(), ""));

      m.put(
          "detalle",
          "Inicio: "
              + Objects.toString(
              r.inicioPeriodoEn(LocalDate.now()),
              "Sin fecha"
          )
              + " · "
              + Objects.toString(
              r.getPeriodicidad(), "Sin periodicidad"
          ).toLowerCase(Locale.ROOT)
      );
    } else {
      String motivo = ((NecesidadExtraordinaria) n).getMotivo();

      m.put("tipo", "Extraordinaria");
      m.put("motivo", motivo);
      m.put("detalle", "Motivo: " + motivo);
    }

    return m;
  }

  private Map<String, Object> datosDonacion(Donacion d) {
    Map<String, Object> m = new HashMap<>();

    m.put("id", d.getIdAsString());
    m.put("descripcion", d.getDescripcionGeneral());
    m.put("cantidad", d.getCantidad());
    m.put("unidad", Objects.toString(d.getUnidadMedida(), ""));
    m.put("estado", etiqueta(d.getEstadoActual()));
    m.put("claseEstado", "dt-estado-" + d.getEstadoActual().name());

    m.put(
        "recibida",
        d.getEstadoActual() == EstadoDonacion.ENTREGADA
    );

    m.put(
        "puedeRecibir",
        d.getEstadoActual() == EstadoDonacion.EN_TRASLADO
    );

    m.put(
        "subcategoria",
        d.getSubcategoria() == null
            ? "Sin subcategoría"
            : d.getSubcategoria().getNombre()
    );

    m.put(
        "historial",
        d.getHistorialEstados().stream()
            .map(h -> Map.of(
                "estado", etiqueta(h.getEstadoNuevo()),
                "fecha", Objects.toString(
                    h.getFechaYHora(), ""
                ).replace('T', ' '),
                "motivo", Objects.toString(h.getJustificativo(), "")
            ))
            .toList()
    );

    return m;
  }

  private static List<Map<String, Object>> opciones(
      List<String> valores,
      String actual
  ) {
    return valores.stream()
        .map(v -> Map.<String, Object>of(
            "nombre", v,
            "elegido", v.equals(actual)
        ))
        .toList();
  }

  private static String texto(String s) {
    return Objects.toString(s, "").trim();
  }

  private static String campo(Context c, String n) {
    return texto(c.formParam(n));
  }

  private static String codificar(String s) {
    return URLEncoder.encode(s, StandardCharsets.UTF_8);
  }

  private static String etiqueta(EstadoDonacion e) {
    return switch (e) {
      case EN_DEPOSITO -> "En depósito";
      case ASIGNACION_REALIZADA -> "Asignación realizada";
      case LISTA_PARA_ENTREGAR -> "Lista para entregar";
      case EN_TRASLADO -> "En traslado";
      case ENTREGADA -> "Entregada";
      case ENTREGA_FALLIDA -> "Entrega fallida";
      case VENCIDA -> "Vencida";
    };
  }
}