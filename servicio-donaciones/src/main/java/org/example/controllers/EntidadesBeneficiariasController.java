package org.example.controllers;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Necesidad;
import org.example.dominio.beneficiario.NecesidadRecurrente;

public class EntidadesBeneficiariasController {

  private static final int TAMANIO_PAGINA = 8;

  private final Supplier<EntityManager> entityManagerSupplier;
  private final MisDonacionesController misDonaciones;

  public EntidadesBeneficiariasController(
      Supplier<EntityManager> entityManagerSupplier,
      MisDonacionesController misDonaciones
  ) {
    this.entityManagerSupplier = entityManagerSupplier;
    this.misDonaciones = misDonaciones;
  }

  public void index(Context ctx) {
    EntityManager em = entityManagerSupplier.get();

    String nombre = ctx.queryParam("nombre");
    nombre = nombre == null ? "" : nombre.trim();

    int pagina = obtenerPagina(ctx);

    String condiciones = " FROM EntidadBeneficiaria e";
    boolean hayBusqueda = !nombre.isBlank();

    if (hayBusqueda) {
      condiciones +=
          " WHERE LOWER(e.razonSocial) LIKE :nombre ESCAPE '!'";
    }

    // Escapamos los caracteres especiales de LIKE para buscar texto literal.
    String patron = "%"
        + nombre.toLowerCase(Locale.ROOT)
        .replace("!", "!!")
        .replace("%", "!%")
        .replace("_", "!_")
        + "%";

    var consultaCantidad = em.createQuery(
        "SELECT COUNT(e)" + condiciones,
        Long.class
    );

    if (hayBusqueda) {
      consultaCantidad.setParameter("nombre", patron);
    }

    long total = consultaCantidad.getSingleResult();
    long paginas = Math.max(
        1,
        (total + TAMANIO_PAGINA - 1) / TAMANIO_PAGINA
    );

    pagina = (int) Math.min(pagina, paginas);

    var consulta = em.createQuery(
        "SELECT e" + condiciones
            + " ORDER BY LOWER(e.razonSocial), e.id",
        EntidadBeneficiaria.class
    );

    if (hayBusqueda) {
      consulta.setParameter("nombre", patron);
    }

    List<EntidadBeneficiaria> entidades = consulta
        .setFirstResult((pagina - 1) * TAMANIO_PAGINA)
        .setMaxResults(TAMANIO_PAGINA)
        .getResultList();

    List<Map<String, Object>> tarjetas = new ArrayList<>();

    for (EntidadBeneficiaria entidad : entidades) {
      tarjetas.add(prepararEntidad(entidad));
    }

    Map<String, Object> modelo = new HashMap<>();

    misDonaciones.agregarDatosDonante(modelo, em);

    modelo.put("entidades", tarjetas);
    modelo.put("nombre", nombre);
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

    ctx.render("entidadesBeneficiarias.hbs", modelo);
  }

  private Map<String, Object> prepararEntidad(
      EntidadBeneficiaria entidad
  ) {
    Map<String, Object> tarjeta = new HashMap<>();

    tarjeta.put("id", entidad.getIdAsString());
    tarjeta.put("nombre", entidad.getRazonSocial());
    tarjeta.put("direccion", entidad.getDireccion());

    List<Map<String, Object>> necesidades = new ArrayList<>();

    // Orden estable para que el popup no cambie de orden entre consultas.
    List<Necesidad> ordenadas = entidad.getNecesidades().stream()
        .sorted(java.util.Comparator.comparing(Necesidad::getId))
        .toList();

    for (Necesidad necesidad : ordenadas) {
      Map<String, Object> item = new HashMap<>();

      item.put("descripcion", necesidad.getDescripcion());
      item.put("objetivo", necesidad.getCantidadObjetivo());
      item.put("cubierta", necesidad.getCantidadCubierta());
      item.put(
          "pendiente",
          Math.max(0, necesidad.getCantidadPendiente())
      );
      item.put("satisfecha", necesidad.estaSatisfecha());

      var subcategoria = necesidad.getSubcategoria();

      item.put(
          "subcategoria",
          subcategoria == null
              ? "Sin subcategoría"
              : subcategoria.getNombre()
      );

      item.put(
          "categoria",
          subcategoria == null || subcategoria.getCategoria() == null
              ? "Sin categoría"
              : subcategoria.getCategoria().getNombre()
      );

      if (necesidad instanceof NecesidadRecurrente recurrente) {
        item.put("tipo", "Recurrente");

        if (recurrente.getPeriodicidad() != null) {
          item.put(
              "periodicidad",
              recurrente.getPeriodicidad().name()
                  .replace('_', ' ')
                  .toLowerCase(Locale.ROOT)
          );
        }
      } else {
        item.put("tipo", "Extraordinaria");
      }

      necesidades.add(item);
    }

    tarjeta.put("necesidades", necesidades);
    tarjeta.put("cantidadNecesidades", necesidades.size());

    return tarjeta;
  }

  private int obtenerPagina(Context ctx) {
    String valor = ctx.queryParam("pagina");

    if (valor == null || valor.isBlank()) {
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
}