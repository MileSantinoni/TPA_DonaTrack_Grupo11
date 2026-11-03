package org.example.api.donaciones;

import io.javalin.http.Context;
import java.util.*;
import java.util.function.Supplier;
import javax.persistence.*;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.donacion.Donacion;

public record AvisoPlanificacionController(Supplier<EntityManager> proveedor, RepositorioAsignacionesDonacion repo) {
  public record Aviso(String idLote,List<UUID> donaciones) {}
  public void registrar(Context ctx) {
    String clave=System.getenv("PLANIFICACION_TOKEN");
    if(clave==null||clave.isBlank()||!clave.equals(ctx.header("X-Planificacion-Token"))) {
      ctx.status(403);return;
    }
    var aviso=ctx.bodyAsClass(Aviso.class);
    if(aviso==null||aviso.idLote()==null||aviso.idLote().isBlank()||aviso.donaciones()==null
        ||aviso.donaciones().isEmpty()||aviso.donaciones().size()>100||aviso.donaciones().contains(null)) {
      ctx.status(400);return;
    }
    try {
      repo.ejecutarEnTransaccion(()->{
        var em=proveedor.get();
        for(UUID id:new TreeSet<>(aviso.donaciones())){
          var d=em.find(Donacion.class,id,LockModeType.PESSIMISTIC_WRITE);
          if(d==null) throw new IllegalStateException("Donacion inexistente: "+id);
          d.registrarPlanificacion(aviso.idLote());
        }
        return null;
      });
    } catch(IllegalStateException conflicto) {ctx.status(409).result(conflicto.getMessage());return;}
    ctx.status(204);
  }
}
