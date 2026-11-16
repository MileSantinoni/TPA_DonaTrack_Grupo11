package org.example.api.logistica;

import io.javalin.http.Context;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.persistence.EntityManager;
import org.example.dominio.logistica.EstadoEntrega;
import org.example.dominio.logistica.Ruta;

public class SeguimientoMapaController {

  private final Supplier<EntityManager> proveedor;

  public SeguimientoMapaController(Supplier<EntityManager> proveedor) {
    this.proveedor = proveedor;
  }

  public void consultar(Context ctx) {
    ctx.header("Cache-Control", "no-store");

    String idDonacion = ctx.pathParam("idDonacion");
    EntityManager em = proveedor.get();

    var rutas = em.createQuery(
            """
            SELECT DISTINCT r
            FROM Ruta r
            JOIN r.entregas e
            WHERE e.idDonacion = :idDonacion
            """,
            Ruta.class
        )
        .setParameter("idDonacion", idDonacion)
        .setMaxResults(2)
        .getResultList();

    if (rutas.isEmpty()) {
      ctx.status(404).json(Map.of(
          "mensaje", "Esta donación todavía no tiene una ruta asignada."
      ));
      return;
    }

    if (rutas.size() > 1) {
      ctx.status(409).json(Map.of(
          "mensaje",
          "La donación tiene varias rutas registradas. "
              + "Todavía no se puede determinar cuál mostrar."
      ));
      return;
    }

    Ruta ruta = rutas.get(0);

    var entrega = ruta.getEntregas().stream()
        .filter(e -> idDonacion.equals(e.getIdDonacion()))
        .findFirst()
        .orElseThrow();

    Map<String, Object> respuesta = new HashMap<>();

    respuesta.put("idRuta", ruta.getId());
    respuesta.put("patente", ruta.getCamion().getPatente());
    respuesta.put("idEntidad", entrega.getIdEntidad());
    respuesta.put("entidad", entrega.getRazonSocial());
    respuesta.put("direccion", entrega.getDireccion());
    respuesta.put("estadoEntrega", entrega.getEstado().name());
    respuesta.put("fechaPlanificada", ruta.getFechaPlanificada() == null ? "" : ruta.getFechaPlanificada().toString());
    respuesta.put("fotos", entrega.getFotosRecepcion());

    var deposito = ruta.getUbicacionDeposito();

    if (deposito != null) {
      respuesta.put("deposito", Map.of(
          "lat", deposito.getLatitud(),
          "lng", deposito.getLongitud()
      ));
    }

    /*
     * No mostramos la posición actual del camión para una entrega
     * terminada: podría estar realizando otra ruta.
     */
    if (ruta.estaActiva()
        && entrega.getEstado() == EstadoEntrega.EN_TRASLADO) {

      long rutasEnTraslado = em.createQuery(
              """
              SELECT COUNT(DISTINCT r)
              FROM Ruta r
              JOIN r.entregas e
              WHERE r.camion.patente = :patente
                AND e.estado = :estado
              """,
              Long.class
          )
          .setParameter("patente", ruta.getCamion().getPatente())
          .setParameter("estado", EstadoEntrega.EN_TRASLADO)
          .getSingleResult();

      var ubicacion = ruta.getCamion().getUltimaUbicacion();

      boolean fechaCompatible = ubicacion != null
          && ubicacion.getFechaYHora() != null
          && (ruta.getFechaPlanificada() == null
          || !ubicacion.getFechaYHora().toLocalDate()
          .isBefore(ruta.getFechaPlanificada()));

      if (rutasEnTraslado == 1 && fechaCompatible) {
        respuesta.put("camion", Map.of(
            "lat", ubicacion.getLatitud(),
            "lng", ubicacion.getLongitud(),
            "fecha", ubicacion.getFechaYHora().toString()
        ));
      }
    }

    ctx.json(respuesta);
  }
}