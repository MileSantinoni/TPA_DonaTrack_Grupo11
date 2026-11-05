package org.example.controllers;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.ServiceUnavailableResponse;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.PersonaHumana;

public class MisDonacionesController {

  private static final int TAMANIO_PAGINA = 10;

  private static final DateTimeFormatter FECHA =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private static final DateTimeFormatter FECHA_HORA =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

  private final Supplier<EntityManager> entityManagerSupplier;

  private final java.net.http.HttpClient clienteLogistica =
      java.net.http.HttpClient.newBuilder()
          .connectTimeout(java.time.Duration.ofSeconds(3))
          .build();

  private final com.fasterxml.jackson.databind.ObjectMapper jsonMapa =
      new com.fasterxml.jackson.databind.ObjectMapper();

  public MisDonacionesController(
      Supplier<EntityManager> entityManagerSupplier
  ) {
    this.entityManagerSupplier = entityManagerSupplier;
  }

  public void index(Context ctx) {
    EntityManager em = entityManagerSupplier.get();
    Donante donante = obtenerDonanteDeDesarrollo(em);

    String estado = parametro(ctx, "estado");
    String categoria = parametro(ctx, "categoria");
    String subcategoria = parametro(ctx, "subcategoria");
    int pagina = obtenerPagina(ctx);

    Map<String, Object> parametros = new HashMap<>();
    parametros.put("donanteId", donante.getId());

    String condiciones = """
        FROM Donacion d
        LEFT JOIN d.subcategoria s
        LEFT JOIN s.categoria c
        WHERE d.donante.id = :donanteId
        """;

    if (!estado.isBlank()) {
      try {
        EstadoDonacion.valueOf(estado);
      } catch (IllegalArgumentException e) {
        throw new BadRequestResponse("Estado de donación inválido.");
      }

      condiciones += " AND d.estadoActualTexto = :estado";
      parametros.put("estado", estado);
    }

    if (!categoria.isBlank()) {
      condiciones += " AND c.nombre = :categoria";
      parametros.put("categoria", categoria);
    }

    if (!subcategoria.isBlank()) {
      condiciones += " AND s.nombre = :subcategoria";
      parametros.put("subcategoria", subcategoria);
    }

    var consultaCantidad = em.createQuery(
        "SELECT COUNT(d) " + condiciones,
        Long.class
    );

    parametros.forEach(consultaCantidad::setParameter);

    long total = consultaCantidad.getSingleResult();
    long paginas = Math.max(
        1,
        (total + TAMANIO_PAGINA - 1) / TAMANIO_PAGINA
    );

    pagina = (int) Math.min(pagina, paginas);

    var consulta = em.createQuery(
        "SELECT d " + condiciones
            + " ORDER BY d.fechaDeRegistro DESC, d.id",
        Donacion.class
    );

    parametros.forEach(consulta::setParameter);

    List<Donacion> donaciones = consulta
        .setFirstResult((pagina - 1) * TAMANIO_PAGINA)
        .setMaxResults(TAMANIO_PAGINA)
        .getResultList();

    List<Map<String, Object>> filas = new ArrayList<>();

    for (Donacion donacion : donaciones) {
      filas.add(prepararDonacion(em, donacion));
    }

    Map<String, Object> modelo = new HashMap<>();

    agregarDatosDonante(modelo, donante);

    modelo.put("donaciones", filas);
    modelo.put("total", total);
    modelo.put("pagina", pagina);
    modelo.put("paginas", paginas);
    modelo.put("hayPaginacion", paginas > 1);

    if (pagina > 1) {
      modelo.put("anterior", pagina - 1);
    }

    if (pagina < paginas) {
      modelo.put("siguiente", pagina + 1);
    }

    modelo.put("estado", estado);
    modelo.put("categoria", categoria);
    modelo.put("subcategoria", subcategoria);

    modelo.put(
        "estados",
        Arrays.stream(EstadoDonacion.values())
            .map(e -> Map.of(
                "valor", e.name(),
                "nombre", nombreEstado(e),
                "elegido", e.name().equals(estado)
            ))
            .toList()
    );

    List<String> categorias = em.createQuery(
        "SELECT DISTINCT c.nombre FROM Categoria c ORDER BY c.nombre",
        String.class
    ).getResultList();

    modelo.put(
        "categorias",
        opciones(categorias, categoria)
    );

    List<String> subcategorias = em.createQuery(
        "SELECT DISTINCT s.nombre FROM Subcategoria s ORDER BY s.nombre",
        String.class
    ).getResultList();

    modelo.put(
        "subcategorias",
        opciones(subcategorias, subcategoria)
    );

    modelo.put(
        "googleMapsKey",
        System.getenv().getOrDefault("GOOGLE_MAPS_API_KEY", "")
    );

    ctx.render("misDonaciones.hbs", modelo);
  }

  private Map<String, Object> prepararDonacion(
      EntityManager em,
      Donacion donacion
  ) {
    Map<String, Object> fila = new HashMap<>();

    fila.put("id", donacion.getIdAsString());
    fila.put("descripcion", donacion.getDescripcionGeneral());
    fila.put("cantidad", donacion.getCantidad());
    fila.put("unidad", donacion.getUnidadMedida());
    fila.put("estado", nombreEstado(donacion.getEstadoActual()));
    fila.put("claseEstado", "dt-estado-" + donacion.getEstadoActual().name());

    fila.put(
        "fecha",
        donacion.getFechaDeRegistro() == null
            ? "Sin fecha"
            : donacion.getFechaDeRegistro().format(FECHA)
    );

    var subcategoria = donacion.getSubcategoria();

    fila.put(
        "subcategoria",
        subcategoria == null ? "Sin subcategoría" : subcategoria.getNombre()
    );

    fila.put(
        "categoria",
        subcategoria == null || subcategoria.getCategoria() == null
            ? "Sin categoría"
            : subcategoria.getCategoria().getNombre()
    );

    List<AsignacionDonacion> asignaciones = em.createQuery(
            """
            SELECT a
            FROM AsignacionDonacion a
            JOIN FETCH a.entidad
            WHERE a.donacion.id = :donacionId
            """,
            AsignacionDonacion.class
        )
        .setParameter("donacionId", donacion.getId())
        .getResultList();

    fila.put(
        "entidades",
        asignaciones.stream()
            .map(a -> a.getEntidad().getRazonSocial())
            .distinct()
            .sorted()
            .toList()
    );

    List<Map<String, Object>> historial = new ArrayList<>();

    for (var cambio : donacion.getHistorialEstados()) {
      Map<String, Object> item = new HashMap<>();

      item.put("estado", nombreEstado(cambio.getEstadoNuevo()));
      item.put(
          "fecha",
          cambio.getFechaYHora() == null
              ? ""
              : cambio.getFechaYHora().format(FECHA_HORA)
      );
      item.put("motivo", cambio.getJustificativo());

      historial.add(item);
    }

    fila.put("historial", historial);

    return fila;
  }

  private Donante obtenerDonanteDeDesarrollo(EntityManager em) {
    String configurado = System.getenv("DONATRACK_DEV_DONANTE_ID");
    Long id;

    try {
      id = Long.valueOf(configurado);
    } catch (NumberFormatException e) {
      throw new ServiceUnavailableResponse(
          "Configurá DONATRACK_DEV_DONANTE_ID con el ID numérico "
              + "de un donante existente."
      );
    }

    Donante donante = em.find(Donante.class, id);

    if (donante == null) {
      throw new ServiceUnavailableResponse(
          "El donante indicado en DONATRACK_DEV_DONANTE_ID no existe."
      );
    }

    return donante;
  }

  private int obtenerPagina(Context ctx) {
    String valor = parametro(ctx, "pagina");

    if (valor.isBlank()) {
      return 1;
    }

    try {
      int pagina = Integer.parseInt(valor);

      if (pagina < 1 || pagina > 100000) {
        throw new NumberFormatException();
      }

      return pagina;
    } catch (NumberFormatException e) {
      throw new BadRequestResponse("Número de página inválido.");
    }
  }

  private String parametro(Context ctx, String nombre) {
    String valor = ctx.queryParam(nombre);
    return valor == null ? "" : valor.trim();
  }

  private List<Map<String, Object>> opciones(
      List<String> nombres,
      String seleccionado
  ) {
    List<Map<String, Object>> opciones = new ArrayList<>();

    for (String nombre : nombres) {
      if (nombre == null) {
        continue;
      }

      Map<String, Object> opcion = new HashMap<>();
      opcion.put("nombre", nombre);
      opcion.put("elegido", nombre.equals(seleccionado));
      opciones.add(opcion);
    }

    return opciones;
  }

  private static String nombreEstado(EstadoDonacion estado) {
    return switch (estado) {
      case EN_DEPOSITO -> "En depósito";
      case ASIGNACION_REALIZADA -> "Asignación realizada";
      case LISTA_PARA_ENTREGAR -> "Lista para entregar";
      case EN_TRASLADO -> "En traslado";
      case ENTREGADA -> "Entregada";
      case ENTREGA_FALLIDA -> "Entrega fallida";
      case VENCIDA -> "Vencida";
    };
  }

  public void agregarDatosDonante(
      Map<String, Object> modelo,
      EntityManager em
  ) {
    agregarDatosDonante(modelo, obtenerDonanteDeDesarrollo(em));
  }

  private void agregarDatosDonante(
      Map<String, Object> modelo,
      Donante donante
  ) {
    String nombre = donante instanceof PersonaHumana persona
        ? persona.getNombre()
        : donante.getMail();

    if (nombre == null || nombre.isBlank()) {
      nombre = "Donante";
    }

    modelo.put("usuarioNombre", nombre);
    modelo.put(
        "usuarioInicial",
        nombre.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
    );
  }

  public void seguimiento(Context ctx) {
    ctx.header("Cache-Control", "no-store");

    EntityManager em = entityManagerSupplier.get();
    Donante donante = obtenerDonanteDeDesarrollo(em);

    java.util.UUID id;

    try {
      id = java.util.UUID.fromString(ctx.pathParam("id"));
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("ID de donación inválido.");
    }

    Donacion donacion = em.find(Donacion.class, id);

    // El endpoint solo devuelve seguimiento del donante seleccionado.
    if (donacion == null
        || donacion.getDonante() == null
        || !donante.getId().equals(donacion.getDonante().getId())) {

      ctx.status(404).json(Map.of(
          "mensaje", "No se encontró la donación."
      ));
      return;
    }

    String base = System.getenv().getOrDefault(
        "LOGISTICA_URL",
        "http://localhost:8082"
    ).replaceAll("/+$", "");

    var request = java.net.http.HttpRequest.newBuilder(
            java.net.URI.create(
                base + "/interno/donaciones/" + id + "/seguimiento"
            )
        )
        .timeout(java.time.Duration.ofSeconds(5))
        .GET()
        .build();

    try {
      var respuesta = clienteLogistica.send(
          request,
          java.net.http.HttpResponse.BodyHandlers.ofString()
      );

      if (respuesta.statusCode() == 404) {
        ctx.status(404).json(Map.of(
            "mensaje", "Esta donación todavía no tiene una ruta asignada."
        ));
        return;
      }

      if (respuesta.statusCode() == 409) {
        ctx.status(409).json(Map.of(
            "mensaje",
            "Hay varias rutas registradas para esta donación. "
                + "No se puede determinar cuál mostrar."
        ));
        return;
      }

      if (respuesta.statusCode() != 200) {
        ctx.status(502).json(Map.of(
            "mensaje", "No se pudo consultar el seguimiento en logística."
        ));
        return;
      }

      var contenido = jsonMapa.readTree(respuesta.body());

      if (contenido == null || !contenido.isObject()) {
        throw new java.io.IOException("Respuesta inválida de logística");
      }

      var datos = (com.fasterxml.jackson.databind.node.ObjectNode) contenido;
      String idEntidad = datos.path("idEntidad").asText();

      /*
       * Usamos la entidad indicada por la entrega de logística,
       * comprobando que esté asociada a esta donación.
       */
      var asignaciones = em.createQuery(
              """
              SELECT a
              FROM AsignacionDonacion a
              JOIN FETCH a.entidad
              WHERE a.donacion.id = :id
              """,
              AsignacionDonacion.class
          )
          .setParameter("id", id)
          .getResultList();

      var entidad = asignaciones.stream()
          .map(AsignacionDonacion::getEntidad)
          .filter(e -> e.getIdAsString().equals(idEntidad))
          .findFirst();

      if (entidad.isEmpty()) {
        ctx.status(409).json(Map.of(
            "mensaje",
            "La entidad de la ruta no coincide con las asignaciones "
                + "de la donación."
        ));
        return;
      }

      var destino = entidad.get();

      if (destino.getLatitud() != null
          && destino.getLongitud() != null) {
        datos.putObject("destino")
            .put("lat", destino.getLatitud())
            .put("lng", destino.getLongitud());
      }

      ctx.contentType("application/json");
      ctx.result(jsonMapa.writeValueAsString(datos));

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();

      ctx.status(503).json(Map.of(
          "mensaje", "La consulta de seguimiento fue interrumpida."
      ));
    } catch (java.io.IOException e) {
      ctx.status(503).json(Map.of(
          "mensaje",
          "Logística no está disponible. Podés volver a intentar."
      ));
    }
  }
}