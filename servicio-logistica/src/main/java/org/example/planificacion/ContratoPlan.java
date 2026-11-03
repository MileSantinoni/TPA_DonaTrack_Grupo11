package org.example.planificacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.example.integracion.*;

public final class ContratoPlan {
  private ContratoPlan() {}
  public static final ObjectMapper JSON = new ObjectMapper();
  public static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  public static final String CABECERA = "X-Planificacion-Token";
  public record Carga(double pesoKg, double volumenM3) {}
  public record Pedido(Double latitudDeposito, Double longitudDeposito, Map<String,Carga> cargas) {}
  public record Lote(String id, String fecha, double latitud, double longitud,
      List<String> patentes, List<AsignacionDisponible> destinos, ClienteHeiGit.Solicitud solicitud) {}
  public record Respuesta(String id, com.fasterxml.jackson.databind.JsonNode resultado) {}
  public record RutaGuardada(String idRuta,String patente,List<String> entregas) {}
  public record Recibo(List<RutaGuardada> rutas,List<String> sinAsignar) {}
  public static String env(String nombre,String fallback) {
    return System.getenv().getOrDefault(nombre,fallback);
  }
  public static String token() {
    String s=System.getenv("PLANIFICACION_TOKEN");
    if(s==null || s.isBlank()) throw new IllegalStateException("Falta PLANIFICACION_TOKEN");
    return s;
  }
  public static void post(String url,Object dato) throws IOException,InterruptedException {
    var req=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(120))
        .header(CABECERA,token()).header("Content-Type","application/json")
        .POST(HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(dato))).build();
    var r=HTTP.send(req,HttpResponse.BodyHandlers.ofString());
    if(r.statusCode()/100!=2) throw new IOException("HTTP "+r.statusCode()+" al llamar a "+url);
  }
}
