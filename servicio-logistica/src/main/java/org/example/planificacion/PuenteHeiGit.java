package org.example.planificacion;

import io.javalin.Javalin;
import org.example.integracion.ClienteHeiGit;
import static org.example.planificacion.ContratoPlan.*;

// Adaptador de protocolo. El cálculo de todas las rutas lo hace HeiGIT.
public class PuenteHeiGit {
  public static void main(String[] args) {
    var cliente=new ClienteHeiGit(System.getenv("HEIGIT_API_KEY"));
    String secreto=token();
    String callback=env("PLAN_CALLBACK_URL","http://localhost:8082/interno/planificaciones/resultado");
    var app=Javalin.create();
    app.post("/lotes",ctx -> {
      if(!secreto.equals(ctx.header(CABECERA))) {ctx.status(403);return;}
      var lote=ctx.bodyAsClass(Lote.class);
      if(lote.solicitud().jobs().size()>100) {ctx.status(400);return;}
      var resultado=cliente.optimizar(lote.solicitud());
      post(callback,new Respuesta(lote.id(),resultado));
      ctx.status(204);
    });
    app.start(8084);
  }
}
