package org.example.planificacion;

import com.fasterxml.jackson.databind.JsonNode;
import io.javalin.http.Context;
import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.Supplier;
import javax.persistence.*;
import org.example.Repositorios.*;
import org.example.dominio.logistica.*;
import org.example.integracion.*;
import static org.example.planificacion.ContratoPlan.*;

public class PlanificacionController {
  private final Supplier<EntityManager> actual;
  private final ClienteDonaciones donaciones;
  private final String urlDonaciones;
  private static final int MAX_TRABAJOS = 50; // limite conservador inferior al máximo de la consigna
  private static final int MAX_CAMIONES = 3;

  public PlanificacionController(Supplier<EntityManager> actual, ClienteDonaciones donaciones,String urlDonaciones) {
    this.actual=actual;this.donaciones=donaciones;this.urlDonaciones=urlDonaciones.replaceAll("/+$","");
  }

  private static int unidades(double n,double factor,boolean demanda) {
    double v=demanda?Math.ceil(n*factor):Math.floor(n*factor);
    if(!Double.isFinite(v)||n<=0||v<1||v>Integer.MAX_VALUE)
      throw new IllegalArgumentException("Peso o volumen fuera de rango");
    return (int)v;
  }
  private static boolean abierta(Ruta r) {
    return r.getEntregas().stream().anyMatch(e->e.getEstado()!=EstadoEntrega.ENTREGADA);
  }

  // Serializa las ejecuciones de esta instancia; el callback NO adquiere este monitor.
  public synchronized void ejecutar(Context ctx) throws Exception {
    token();
    Pedido pedido=ctx.bodyAsClass(Pedido.class);
    if(pedido==null||pedido.latitudDeposito()==null||pedido.longitudDeposito()==null
        ||!Double.isFinite(pedido.latitudDeposito())||!Double.isFinite(pedido.longitudDeposito())
        ||Math.abs(pedido.latitudDeposito())>90||Math.abs(pedido.longitudDeposito())>180)
      throw new IllegalArgumentException("Indique las coordenadas del deposito");

    EntityManager em=actual.get();
    var recibos=new ArrayList<Recibo>();
    var intentadas=new HashSet<String>();
    // Reanudar antes de crear otro lote: caída del puente, callback perdido o aviso pendiente.
    var pendientes=em.createQuery("select l.id from LotePlanificado l where l.avisado=false order by l.id",String.class)
        .getResultList();
    for(String id:pendientes) {
      Lote previo=JSON.readValue(em.find(LotePlanificado.class,id).solicitud,Lote.class);
      previo.destinos().forEach(a->intentadas.add(a.idDonacion()));
      recibos.add(completar(id));
    }
    var asignaciones=donaciones.listarAsignaciones();
    var sinUbicacion=new ArrayList<String>();
    var sinCarga=new ArrayList<String>();
    var cargas=new HashMap<String,Carga>();
    // Recuperar medidas de trabajos previamente devueltos como no asignados.
    for(var f:em.createQuery("select l from LotePlanificado l where l.recibo is not null",LotePlanificado.class).getResultList()){
      var recibo=JSON.readValue(f.recibo,Recibo.class);
      if(recibo.sinAsignar().isEmpty())continue;
      var previo=JSON.readValue(f.solicitud,Lote.class);
      for(var j:previo.solicitud().jobs()){
        String id=previo.destinos().get(j.id()-1).idDonacion();
        if(recibo.sinAsignar().contains(id))
          cargas.put(id,new Carga(j.delivery().get(0)/1000.0,j.delivery().get(1)/1000.0));
      }
    }
    if(pedido.cargas()!=null)cargas.putAll(pedido.cargas());
    var candidatos=new ArrayList<AsignacionDisponible>();
    var vistas=new HashSet<String>();
    var rutas=new RepositorioRutas(em);
    var anteriores=rutas.buscarTodos();
    var enRuta=new HashSet<String>();
    anteriores.forEach(r->r.getEntregas().forEach(e->enRuta.add(e.getIdDonacion())));
    for(var a:asignaciones) {
      if(!"ASIGNACION_REALIZADA".equals(a.estadoDonacion())||enRuta.contains(a.idDonacion())
          ||intentadas.contains(a.idDonacion())) continue;
      if(!vistas.add(a.idDonacion())) throw new IllegalStateException("Asignacion duplicada: "+a.idDonacion());
      if(!a.tieneUbicacion()){sinUbicacion.add(a.idDonacion());continue;}
      if(cargas.get(a.idDonacion())==null){sinCarga.add(a.idDonacion());continue;}
      candidatos.add(a);
    }
    String fecha=LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires")).plusDays(1).toString();
    var deposito=List.of(pedido.longitudDeposito(),pedido.latitudDeposito());
    var sinCamion=new ArrayList<String>();
    for(int i=0;i<candidatos.size();i+=MAX_TRABAJOS) {
      var destinos=List.copyOf(candidatos.subList(i,Math.min(i+MAX_TRABAJOS,candidatos.size())));
      var probados=new HashSet<String>();
      while(!destinos.isEmpty()) {
      em.clear();
      var ocupados=new HashSet<String>();
      rutas.buscarTodos().stream().filter(PlanificacionController::abierta)
          .forEach(r->ocupados.add(r.getCamion().getPatente()));
      var flota=new RepositorioCamiones(em).buscarDisponibles().stream()
          .filter(c->!ocupados.contains(c.getPatente()) && !probados.contains(c.getPatente())).sorted(Comparator.comparing(Camion::getPatente))
          .limit(MAX_CAMIONES).toList();
      if(flota.isEmpty()){
        if(probados.isEmpty()) destinos.forEach(a->sinCamion.add(a.idDonacion()));
        break;
      }
      flota.forEach(c->probados.add(c.getPatente()));

      var jobs=new ArrayList<ClienteHeiGit.Trabajo>();
      for(var a:destinos) {
        var carga=cargas.get(a.idDonacion());
        jobs.add(new ClienteHeiGit.Trabajo(jobs.size()+1,a.idDonacion(),a.coordenadasParaHeiGit(),
            List.of(unidades(carga.pesoKg(),1000,true),unidades(carga.volumenM3(),1000,true)),0));
      }
      var vehicles=new ArrayList<ClienteHeiGit.Vehiculo>();
      for(var c:flota) vehicles.add(new ClienteHeiGit.Vehiculo(vehicles.size()+1,c.getPatente(),
          "driving-hgv",deposito,deposito,List.of(unidades(c.getCapacidadCarga(),1000,false),
          unidades(c.getCapacidadVolumen(),1000,false))));
      var lote=new Lote(UUID.randomUUID().toString(),fecha,pedido.latitudDeposito(),pedido.longitudDeposito(),
          flota.stream().map(Camion::getPatente).toList(),destinos,new ClienteHeiGit.Solicitud(jobs,vehicles));
      String payload=JSON.writeValueAsString(lote);
      rutas.enTransaccion(()->em.persist(new LotePlanificado(lote.id(),payload)));
      var recibido=completar(lote.id());
      recibos.add(recibido);
      destinos=destinos.stream().filter(a->recibido.sinAsignar().contains(a.idDonacion())).toList();
      }
    }
    ctx.json(Map.of("fecha",fecha,"lotes",recibos,"sinUbicacion",sinUbicacion,"sinDatosDeCarga",sinCarga,
        "sinCamionDisponible",sinCamion));
  }

  private Recibo completar(String id) throws Exception {
    var em=actual.get();
    var fila=em.find(LotePlanificado.class,id);
    if(fila.recibo==null) {
      post(env("PLAN_PUENTE_URL","http://localhost:8084/lotes"),JSON.readValue(fila.solicitud,Lote.class));
      em.clear();fila=em.find(LotePlanificado.class,id);
    }
    if(fila.recibo==null) throw new IOException("No se recibio el callback del lote "+id);
    Recibo recibo=JSON.readValue(fila.recibo,Recibo.class);
    var ids=recibo.rutas().stream().flatMap(r->r.entregas().stream()).toList();
    if(!ids.isEmpty()&&!fila.avisado)
      post(urlDonaciones+"/interno/donaciones/planificadas",Map.of("idLote",id,"donaciones",ids));
    var guardada=fila;
    new RepositorioRutas(em).enTransaccion(()->guardada.avisado=true);
    return recibo;
  }

  private static int indice(JsonNode n,int limite) {
    if(!n.isIntegralNumber()||!n.canConvertToInt()||n.intValue()<1||n.intValue()>limite)
      throw new IllegalArgumentException("ID desconocido en el resultado: "+n);
    return n.intValue()-1;
  }

  public void callback(Context ctx) throws Exception {
    if(!token().equals(ctx.header(CABECERA))){ctx.status(403);return;}
    var r=ctx.bodyAsClass(Respuesta.class);
    var em=actual.get();
    new RepositorioRutas(em).enTransaccion(()->{
      var f=em.find(LotePlanificado.class,r.id(),LockModeType.PESSIMISTIC_WRITE);
      if(f==null) throw new IllegalArgumentException("Lote desconocido");
      if(f.recibo!=null) return; // ya aplicado: no duplicar rutas
      try {
        var lote=JSON.readValue(f.solicitud,Lote.class);
        var recibo=guardar(em,lote,r.resultado());
        f.recibo=JSON.writeValueAsString(recibo);
      } catch(IOException e){throw new IllegalArgumentException("Lote invalido",e);}
    });
    ctx.status(204);
  }

  private Recibo guardar(EntityManager em,Lote l,JsonNode res) {
    if(res==null||!res.path("code").isIntegralNumber()||res.path("code").asInt()!=0
        ||!res.path("routes").isArray()||!res.path("unassigned").isArray())
      throw new IllegalArgumentException("Respuesta del optimizador invalida");
    var usados=new HashSet<String>();
    var vistos=new HashSet<Integer>();
    var rutas=new ArrayList<RutaGuardada>();
    var sinAsignar=new ArrayList<String>();
    var repo=new RepositorioRutas(em);
    for(var rr:res.path("routes")) {
      String patente=l.patentes().get(indice(rr.path("vehicle"),l.patentes().size()));
      if(!usados.add(patente)||!rr.path("steps").isArray()) throw new IllegalArgumentException("Ruta invalida");
      var camion=em.find(Camion.class,patente,LockModeType.PESSIMISTIC_WRITE);
      if(camion==null||!camion.estaDisponible()||repo.buscarPorPatente(patente).stream().anyMatch(PlanificacionController::abierta))
        throw new IllegalStateException("El camion ya no esta disponible: "+patente);
      var ruta=new Ruta(camion,new UbicacionCamion(l.latitud(),l.longitud(),0,LocalDateTime.now()));
      ruta.programarPara(LocalDate.parse(l.fecha()));
      var ids=new ArrayList<String>();
      for(var paso:rr.path("steps")) {
        String tipo=paso.path("type").asText();
        if(tipo.equals("start")||tipo.equals("end"))continue;
        if(!tipo.equals("job"))throw new IllegalArgumentException("Paso inesperado: "+tipo);
        int idx=indice(paso.path("id"),l.destinos().size());
        if(!vistos.add(idx))throw new IllegalArgumentException("Trabajo repetido");
        var a=l.destinos().get(idx);
        if(em.createQuery("select count(e) from Entrega e where e.idDonacion=:id",Long.class)
            .setParameter("id",a.idDonacion()).getSingleResult()>0)
          throw new IllegalStateException("Ya existe entrega para "+a.idDonacion());
        ids.add(a.idDonacion());
        ruta.agregarEntrega(new Entrega(a.idDonacion(),a.idEntidad(),a.razonSocial(),a.direccion(),a.telefono(),ids.size()));
      }
      if(ids.isEmpty())continue;
      camion.marcarNoDisponible();em.persist(ruta);
      rutas.add(new RutaGuardada(ruta.getId(),patente,ids));
    }
    for(var item:res.path("unassigned")) {
      int idx=indice(item.path("id"),l.destinos().size());
      if(!vistos.add(idx))throw new IllegalArgumentException("Trabajo duplicado en sin asignar");
      sinAsignar.add(l.destinos().get(idx).idDonacion());
    }
    if(vistos.size()!=l.destinos().size())throw new IllegalArgumentException("Faltan entregas en el resultado");
    return new Recibo(rutas,sinAsignar);
  }

  public void consultar(Context ctx) {
    LocalDate fecha=LocalDate.parse(ctx.pathParam("fecha"));
    var rutas=new RepositorioRutas(actual.get()).buscarPorPatente(ctx.pathParam("patente"));
    ctx.json(rutas.stream().filter(r->fecha.equals(r.getFechaPlanificada())).map(r->Map.of(
        "idRuta",r.getId(),"patente",r.getCamion().getPatente(),"fecha",fecha.toString(),
        "entregas",r.getEntregas().stream().map(e->Map.of("orden",e.getOrden(),"idDonacion",e.getIdDonacion(),
            "direccion",e.getDireccion(),"entidad",e.getRazonSocial())).toList())).toList());
  }
}
