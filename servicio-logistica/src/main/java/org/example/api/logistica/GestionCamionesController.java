package org.example.api.logistica;

import io.javalin.http.*;
import java.util.*;
import java.util.function.Supplier;
import javax.persistence.*;
import org.example.dominio.logistica.*;

public class GestionCamionesController {

  private final Supplier<EntityManager> proveedor;

  private static final String ABIERTA = """
      EXISTS (
        SELECT r.id FROM Ruta r JOIN r.entregas e
        WHERE r.camion = c AND e.estado <> :fin
      )
      """;

  private static final String EN_RUTA = """
      EXISTS (
        SELECT r.id FROM Ruta r JOIN r.entregas e
        WHERE r.camion = c
          AND r.activa = true
          AND e.estado <> :fin
      )
      """;

  public record Datos(
      String patente,
      Double capacidadVolumen,
      Double altura,
      Double capacidadCarga,
      Boolean disponible
  ) {}

  public record Disponibilidad(Boolean disponible) {}

  public GestionCamionesController(
      Supplier<EntityManager> proveedor
  ) {
    this.proveedor = proveedor;
  }

  public void listar(Context ctx) {
    EntityManager em = proveedor.get();

    String q = Objects.toString(
        ctx.queryParam("q"), ""
    ).trim();

    String estado = Objects.toString(
        ctx.queryParam("estado"), ""
    );

    int pagina;

    try {
      pagina = Integer.parseInt(
          Objects.toString(ctx.queryParam("pagina"), "1")
      );

      if (pagina < 1 || pagina > 100000) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException e) {
      throw new BadRequestResponse("Página inválida.");
    }

    String filtro = switch (estado) {
      case "" -> "";

      case "DISPONIBLE" ->
          " AND c.disponible = true AND NOT " + ABIERTA;

      case "NO_DISPONIBLE" ->
          " AND c.disponible = false AND NOT " + ABIERTA;

      case "ASIGNADO" ->
          " AND " + ABIERTA + " AND NOT " + EN_RUTA;

      case "EN_RUTA" ->
          " AND " + EN_RUTA;

      default ->
          throw new BadRequestResponse(
              "Disponibilidad inválida."
          );
    };

    String from = """
         FROM Camion c
         WHERE LOWER(c.patente) LIKE :q ESCAPE '!'
        """ + filtro;

    String patron = "%" + q.toLowerCase(Locale.ROOT)
        .replace("!", "!!")
        .replace("%", "!%")
        .replace("_", "!_") + "%";

    var count = em.createQuery(
            "SELECT COUNT(c)" + from,
            Long.class
        )
        .setParameter("q", patron);

    var query = em.createQuery(
            "SELECT c" + from + " ORDER BY c.patente",
            Camion.class
        )
        .setParameter("q", patron);

    if (!estado.isEmpty()) {
      count.setParameter("fin", EstadoEntrega.ENTREGADA);
      query.setParameter("fin", EstadoEntrega.ENTREGADA);
    }

    long total = count.getSingleResult();
    long paginas = Math.max(1, (total + 9) / 10);

    pagina = (int) Math.min(pagina, paginas);

    List<Map<String, Object>> filas = new ArrayList<>();

    for (Camion c : query
        .setFirstResult((pagina - 1) * 10)
        .setMaxResults(10)
        .getResultList()) {

      List<Ruta> rutas = abiertas(c);

      String etiqueta;

      if (rutas.stream().anyMatch(Ruta::estaActiva)) {
        etiqueta = "En ruta";
      } else if (!rutas.isEmpty()) {
        etiqueta = "Con ruta asignada";
      } else {
        etiqueta = c.estaDisponible()
            ? "Disponible"
            : "No disponible";
      }

      Map<String, Object> fila = new HashMap<>();

      fila.put("patente", c.getPatente());
      fila.put("kg", c.getCapacidadCarga());
      fila.put("m3", c.getCapacidadVolumen());
      fila.put("h", c.getAltura());
      fila.put("estado", etiqueta);
      fila.put("disponible", c.estaDisponible());
      fila.put("editable", rutas.isEmpty());

      fila.put(
          "rutas",
          rutas.stream().map(r -> Map.of(
              "id", r.getId(),
              "fecha", Objects.toString(
                  r.getFechaPlanificada(),
                  "Sin fecha"
              ),
              "entregas", r.getEntregas().stream()
                  .map(e -> Map.of(
                      "orden", e.getOrden(),
                      "donacion", e.getIdDonacion(),
                      "entidad", Objects.toString(
                          e.getRazonSocial(), "Sin entidad"
                      ),
                      "direccion", Objects.toString(
                          e.getDireccion(), ""
                      )
                  ))
                  .toList()
          )).toList()
      );

      filas.add(fila);
    }

    ctx.header("Cache-Control", "no-store")
        .json(Map.of(
            "camiones", filas,
            "total", total,
            "pagina", pagina,
            "paginas", paginas
        ));
  }

  public void guardar(Context ctx) {
    Datos datos = ctx.bodyAsClass(Datos.class);

    if (datos == null || datos.disponible() == null) {
      throw new BadRequestResponse("Datos incompletos.");
    }

    double volumen = positivo(datos.capacidadVolumen());
    double altura = positivo(datos.altura());
    double carga = positivo(datos.capacidadCarga());

    String id = ctx.pathParamMap().get("patente");

    String patente = id == null
        ? Objects.toString(datos.patente(), "")
        .trim()
        .toUpperCase(Locale.ROOT)
        : id;

    if (id == null && !patente.matches("[A-Z0-9-]{3,20}")) {
      throw new BadRequestResponse(
          "Ingresá una patente de 3 a 20 letras o números, sin espacios."
      );
    }

    if (id != null
        && datos.patente() != null
        && !id.equals(datos.patente())) {
      throw new BadRequestResponse(
          "La patente no se puede modificar."
      );
    }

    EntityManager em = proveedor.get();
    var tx = em.getTransaction();

    tx.begin();

    try {
      Camion c;

      if (id == null) {
        if (em.find(Camion.class, patente) != null) {
          throw new ConflictResponse(
              "La patente ya está registrada."
          );
        }

        c = new Camion(patente, volumen, altura, carga);
        em.persist(c);
      } else {
        c = buscarBloqueado(patente);
        exigirSinRuta(c);
      }

      c.actualizarCaracteristicas(volumen, altura, carga);

      if (datos.disponible()) {
        c.marcarDisponible();
      } else {
        c.marcarNoDisponible();
      }

      tx.commit();

      ctx.status(id == null ? 201 : 200)
          .json(Map.of(
              "mensaje",
              "Camión guardado correctamente."
          ));
    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
    }
  }

  public void disponibilidad(Context ctx) {
    Disponibilidad datos =
        ctx.bodyAsClass(Disponibilidad.class);

    if (datos == null || datos.disponible() == null) {
      throw new BadRequestResponse(
          "Indicá la disponibilidad."
      );
    }

    var tx = proveedor.get().getTransaction();
    tx.begin();

    try {
      Camion c = buscarBloqueado(
          ctx.pathParam("patente")
      );

      exigirSinRuta(c);

      if (datos.disponible()) {
        c.marcarDisponible();
      } else {
        c.marcarNoDisponible();
      }

      tx.commit();

      ctx.json(Map.of(
          "mensaje",
          "Disponibilidad actualizada."
      ));
    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
    }
  }

  private Camion buscarBloqueado(String patente) {
    Camion c = proveedor.get().find(
        Camion.class,
        patente,
        LockModeType.PESSIMISTIC_WRITE
    );

    if (c == null) {
      throw new NotFoundResponse("El camión no existe.");
    }

    return c;
  }

  private List<Ruta> abiertas(Camion c) {
    return proveedor.get().createQuery("""
            SELECT DISTINCT r
            FROM Ruta r JOIN r.entregas e
            WHERE r.camion = :camion
              AND e.estado <> :fin
            ORDER BY r.fechaPlanificada, r.id
            """, Ruta.class)
        .setParameter("camion", c)
        .setParameter("fin", EstadoEntrega.ENTREGADA)
        .getResultList();
  }

  private void exigirSinRuta(Camion c) {
    if (!abiertas(c).isEmpty()) {
      throw new ConflictResponse(
          "El camión tiene una ruta con entregas pendientes. "
              + "No se puede modificar ni liberar."
      );
    }
  }

  private static double positivo(Double n) {
    if (n == null || !Double.isFinite(n) || n <= 0) {
      throw new BadRequestResponse(
          "Capacidad de carga, volumen y altura "
              + "deben ser números positivos."
      );
    }

    return n;
  }

  public void eliminar(Context ctx) {
    EntityManager em = proveedor.get();
    var tx = em.getTransaction();

    tx.begin();

    try {
      Camion camion = buscarBloqueado(ctx.pathParam("patente"));

      // Primero muestra el motivo específico si tiene entregas pendientes.
      exigirSinRuta(camion);

      long rutas = em.createQuery("""
            SELECT COUNT(r)
            FROM Ruta r
            WHERE r.camion = :camion
            """, Long.class)
          .setParameter("camion", camion)
          .getSingleResult();

      long entregas = em.createQuery("""
            SELECT COUNT(e)
            FROM Entrega e
            WHERE e.camionResponsable = :camion
            """, Long.class)
          .setParameter("camion", camion)
          .getSingleResult();

      if (rutas > 0 || entregas > 0) {
        throw new ConflictResponse(
            "No se puede eliminar este camión porque tiene "
                + "historial de rutas o entregas."
        );
      }

      em.remove(camion);
      tx.commit();

      ctx.json(Map.of(
          "mensaje",
          "Camión eliminado correctamente."
      ));
    } catch (PersistenceException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      // También cubre una asociación creada por otra operación
      // entre la comprobación y la eliminación.
      if (esReferenciaExistente(e)) {
        throw new ConflictResponse(
            "No se puede eliminar: el camión tiene registros asociados. "
                + "Recargá la lista."
        );
      }

      throw e;
    } catch (RuntimeException e) {
      if (tx.isActive()) {
        tx.rollback();
      }

      throw e;
    }
  }

  private static boolean esReferenciaExistente(Throwable error) {
    for (Throwable causa = error; causa != null; causa = causa.getCause()) {
      if (causa instanceof java.sql.SQLException sql
          && "23503".equals(sql.getSQLState())) {
        return true;
      }
    }

    return false;
  }
}