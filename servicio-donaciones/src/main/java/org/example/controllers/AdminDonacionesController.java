package org.example.controllers;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.example.dominio.algoritmo.AlgoritmoMadre;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.dominio.donante.*;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.algoritmo.AlgoritmoCompatibilidadSemantica;
import org.example.dominio.algoritmo.AlgoritmoPrioridadSubAtendidos;

public class AdminDonacionesController {

  private final Supplier<EntityManager> proveedor;

  public AdminDonacionesController(Supplier<EntityManager> proveedor) {
    this.proveedor = proveedor;
  }

  public void index(Context ctx) {
    EntityManager em = proveedor.get();

    String estado = parametro(ctx, "estado");
    String categoria = parametro(ctx, "categoria");
    String busqueda = parametro(ctx, "q");
    int pagina = pagina(ctx);

    String condiciones = """
        FROM Donacion d
        LEFT JOIN d.subcategoria s
        LEFT JOIN s.categoria c
        WHERE 1 = 1
        """;

    Map<String, Object> parametros = new HashMap<>();

    if (!estado.isBlank()) {
      try {
        EstadoDonacion.valueOf(estado);
      } catch (IllegalArgumentException e) {
        throw new BadRequestResponse("Estado inválido.");
      }

      condiciones += " AND d.estadoActualTexto = :estado";
      parametros.put("estado", estado);
    }

    if (!categoria.isBlank()) {
      condiciones += " AND c.nombre = :categoria";
      parametros.put("categoria", categoria);
    }

    if (!busqueda.isBlank()) {
      condiciones +=
          " AND LOWER(d.descripcionGeneral) LIKE :busqueda ESCAPE '!'";
      parametros.put("busqueda", patron(busqueda));
    }

    var cantidad = em.createQuery(
        "SELECT COUNT(d) " + condiciones, Long.class
    );
    parametros.forEach(cantidad::setParameter);

    long total = cantidad.getSingleResult();
    long paginas = Math.max(1, (total + 9) / 10);
    pagina = (int) Math.min(pagina, paginas);

    var consulta = em.createQuery(
        "SELECT d " + condiciones
            + " ORDER BY d.fechaDeRegistro DESC, d.id",
        Donacion.class
    );
    parametros.forEach(consulta::setParameter);

    List<Map<String, Object>> filas = new ArrayList<>();

    for (Donacion d : consulta.setFirstResult((pagina - 1) * 10)
        .setMaxResults(10).getResultList()) {

      Map<String, Object> fila = new HashMap<>();

      boolean enDeposito = d.getEstadoActual() == EstadoDonacion.EN_DEPOSITO;
      boolean vencida = d.getFechaVencimiento() != null
          && d.getFechaVencimiento().isBefore(LocalDate.now());

      fila.put("id", d.getIdAsString());
      fila.put("descripcion", d.getDescripcionGeneral());
      fila.put("cantidad", d.getCantidad());
      fila.put("unidad", d.getUnidadMedida());
      fila.put("donante", nombre(d.getDonante()));
      fila.put("fecha", Objects.toString(d.getFechaDeRegistro(), ""));
      fila.put("vencimiento", Objects.toString(d.getFechaVencimiento(), ""));
      fila.put("vencida", vencida);
      fila.put("asignable", enDeposito && !vencida);
      fila.put("puedeVencer", enDeposito && vencida);
      fila.put("estado", etiqueta(d.getEstadoActual()));
      fila.put("clase", "dt-estado-" + d.getEstadoActual().name());

      var sub = d.getSubcategoria();

      fila.put("subcategoria", sub == null ? "Sin subcategoría" : sub.getNombre());
      fila.put(
          "categoria",
          sub == null || sub.getCategoria() == null
              ? "Sin categoría" : sub.getCategoria().getNombre()
      );

      filas.add(fila);
    }

    Map<String, Object> modelo = new HashMap<>();

    modelo.put("donaciones", filas);
    modelo.put("total", total);
    modelo.put("q", busqueda);
    modelo.put("estado", estado);
    modelo.put("categoria", categoria);
    modelo.put("pagina", pagina);
    modelo.put("paginas", paginas);
    modelo.put("hayPaginacion", paginas > 1);

    if (pagina > 1) modelo.put("anterior", pagina - 1);
    if (pagina < paginas) modelo.put("siguiente", pagina + 1);

    modelo.put("estados", Arrays.stream(EstadoDonacion.values())
        .map(e -> Map.of(
            "valor", e.name(),
            "nombre", etiqueta(e),
            "elegido", e.name().equals(estado)
        )).toList());

    modelo.put("categorias", em.createQuery(
            "SELECT DISTINCT c.nombre FROM Categoria c ORDER BY c.nombre",
            String.class
        ).getResultList().stream().filter(Objects::nonNull)
        .map(n -> Map.of("nombre", n, "elegido", n.equals(categoria)))
        .toList());

    modelo.put("subcategorias", em.createQuery(
        "SELECT s FROM Subcategoria s ORDER BY s.nombre", Subcategoria.class
    ).getResultList().stream().map(s -> Map.of(
        "id", s.getId(),
        "nombre", s.getNombre(),
        "tipo", s.getTipo().name()
    )).toList());

    ctx.render("adminDonaciones.hbs", modelo);
  }

  public void buscarDonantes(Context ctx) {
    String q = parametro(ctx, "q");

    if (q.length() < 2) {
      throw new BadRequestResponse("Ingresá al menos dos caracteres.");
    }

    // Búsqueda por email o documento; no descargamos todos los donantes.
    var donantes = proveedor.get().createQuery(
            """
            SELECT d FROM Donante d
            WHERE LOWER(d.mail) LIKE :q ESCAPE '!'
               OR LOWER(d.numeroDocumento) LIKE :q ESCAPE '!'
            ORDER BY d.id
            """,
            Donante.class
        ).setParameter("q", patron(q))
        .setMaxResults(20)
        .getResultList();

    ctx.json(donantes.stream().map(d -> Map.of(
        "id", d.getId().toString(),
        "nombre", nombre(d),
        "mail", Objects.toString(d.getMail(), ""),
        "documento", Objects.toString(d.getNumeroDocumento(), "")
    )).toList());
  }

  public void propuestas(Context ctx) {
    UUID id;

    try {
      id = UUID.fromString(ctx.pathParam("id"));
    } catch (IllegalArgumentException e) {
      throw new BadRequestResponse("ID inválido.");
    }

    EntityManager em = proveedor.get();
    Donacion donacion = em.find(Donacion.class, id);

    if (donacion == null) throw new NotFoundResponse("No existe la donación.");

    if (donacion.getEstadoActual() != EstadoDonacion.EN_DEPOSITO) {
      ctx.status(409).result("La donación ya no está disponible para asignar.");
      return;
    }

    if (donacion.getSubcategoria() == null) {
      ctx.status(409).result("La donación no tiene subcategoría.");
      return;
    }

    if (donacion.getFechaVencimiento() != null
        && donacion.getFechaVencimiento().isBefore(LocalDate.now())) {
      ctx.status(409).result("La donación tiene el vencimiento superado.");
      return;
    }

    var entidades = em.createQuery(
        "SELECT e FROM EntidadBeneficiaria e",
        EntidadBeneficiaria.class
    ).getResultList();

    var repositorioAsignaciones =
        new RepositorioAsignacionesDonacion(em);

    var algoritmo = new AlgoritmoMadre(
        new AlgoritmoCompatibilidadSemantica(),
        new AlgoritmoPrioridadSubAtendidos(repositorioAsignaciones)
    );

    var propuestas = algoritmo.proponer(donacion, entidades);

    ctx.json(propuestas.stream().map(e -> Map.of(
        "id", e.getIdAsString(),
        "nombre", e.getRazonSocial(),
        "direccion", Objects.toString(e.getDireccion(), "")
    )).toList());
  }

  private static String nombre(Donante donante) {
    if (donante == null) return "Sin donante";
    if (donante instanceof PersonaHumana p) {
      return p.getNombre() + " " + p.getApellido();
    }
    if (donante instanceof PersonaJuridica p) {
      return p.getRazonSocial();
    }
    return Objects.toString(donante.getMail(), "Donante");
  }

  private static String patron(String texto) {
    return "%" + texto.toLowerCase(Locale.ROOT)
        .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
  }

  private static String parametro(Context ctx, String nombre) {
    return Objects.toString(ctx.queryParam(nombre), "").trim();
  }

  private static int pagina(Context ctx) {
    String texto = parametro(ctx, "pagina");

    if (texto.isBlank()) return 1;

    try {
      int pagina = Integer.parseInt(texto);
      if (pagina < 1 || pagina > 100000) throw new NumberFormatException();
      return pagina;
    } catch (NumberFormatException e) {
      throw new BadRequestResponse("Página inválida.");
    }
  }

  private static String etiqueta(EstadoDonacion estado) {
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
}