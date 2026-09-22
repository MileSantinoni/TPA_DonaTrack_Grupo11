package org.example.integracion;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Repositorios.RepositorioCamiones;
import org.example.api.logistica.dto.RutaRequest;

import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.UbicacionCamion;



public class ClienteGeneradorRutas {

  private static final String URL_GENERADOR_RUTAS = "http://localhost:8081/rutas/generar";

  private final HttpClient http;
  private final ObjectMapper json = new ObjectMapper();
  private final String urlGeneradorRutas;
  private final RepositorioCamiones repositorioCamiones;
  private final ClienteDonaciones donaciones;

  public ClienteGeneradorRutas() {
    this(
        HttpClient.newHttpClient(),
        System.getenv().getOrDefault("GENERADOR_RUTAS_URL", URL_GENERADOR_RUTAS),
        RepositorioCamiones.getInstance(),
        new ClienteDonaciones(System.getenv().getOrDefault("DONACIONES_URL", "http://localhost:8080"))
    );
  }

  public ClienteGeneradorRutas(
      HttpClient http,
      String urlGeneradorRutas,
      RepositorioCamiones repositorioCamiones,
      ClienteDonaciones donaciones
  ) {
    this.http = http;
    this.urlGeneradorRutas = urlGeneradorRutas;
    this.repositorioCamiones = repositorioCamiones;
    this.donaciones = donaciones;
  }

  public List<RutaRequest> solicitarRutasDesdeRepositorios(UbicacionCamion ubicacionDeposito) throws IOException, InterruptedException {
    return solicitarRutas(
        repositorioCamiones.buscarDisponibles(),
        donaciones.listarAsignaciones(),
        ubicacionDeposito
    );
  }

  public List<RutaRequest> solicitarRutas(
      List<Camion> camionesDisponibles,
      List<AsignacionDisponible> asignaciones,
      UbicacionCamion ubicacionDeposito
  ) throws IOException, InterruptedException {
    if (camionesDisponibles.isEmpty() || asignaciones.isEmpty()) {
      return new ArrayList<>();
    }

    GenerarRutasRequest request =
        armarRequest(camionesDisponibles, asignaciones, ubicacionDeposito);

    HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(urlGeneradorRutas))
        .timeout(java.time.Duration.ofSeconds(30)).header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(request))).build();
    HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) throw new IOException("Generador respondio " + response.statusCode());
    return convertirRespuesta(json.readValue(response.body(), RutaGeneradaResponse[].class));
  }

  public GenerarRutasRequest armarRequest(
      List<Camion> camionesDisponibles,
      List<AsignacionDisponible> asignaciones,
      UbicacionCamion ubicacionDeposito
  ) {
    GenerarRutasRequest request = new GenerarRutasRequest();
    request.setUbicacionDeposito(crearUbicacionRequest(ubicacionDeposito));
    request.setCamiones(crearCamionesRequest(camionesDisponibles));
    request.setAsignaciones(crearAsignacionesRequest(asignaciones));
    return request;
  }

  public List<RutaRequest> convertirRespuesta(RutaGeneradaResponse[] rutasGeneradas) {
    List<RutaRequest> rutas = new ArrayList<>();

    if (rutasGeneradas == null) {
      return rutas;
    }

    for (RutaGeneradaResponse rutaGenerada : rutasGeneradas) {
      rutas.add(convertirRuta(rutaGenerada));
    }

    return rutas;
  }

  private RutaRequest convertirRuta(RutaGeneradaResponse rutaGenerada) {
    RutaRequest ruta = new RutaRequest();
    ruta.setPatente(rutaGenerada.getPatenteCamion());
    ruta.setEntregas(convertirEntregas(rutaGenerada.getEntregas()));

    if (rutaGenerada.getUbicacionDeposito() != null) {
      ruta.setLatitudDeposito(rutaGenerada.getUbicacionDeposito().getLatitud());
      ruta.setLongitudDeposito(rutaGenerada.getUbicacionDeposito().getLongitud());
    }

    return ruta;
  }

  private List<RutaRequest.EntregaRequest> convertirEntregas(
      List<EntregaGeneradaResponse> entregasGeneradas
  ) {
    List<RutaRequest.EntregaRequest> entregas = new ArrayList<>();

    if (entregasGeneradas == null) {
      return entregas;
    }

    for (EntregaGeneradaResponse entregaGenerada : entregasGeneradas) {
      RutaRequest.EntregaRequest entrega = new RutaRequest.EntregaRequest();
      entrega.setIdDonacion(entregaGenerada.getIdDonacion());
      entrega.setRazonSocial(entregaGenerada.getRazonSocialEntidad());
      entrega.setDireccion(entregaGenerada.getDireccionEntidad());
      entrega.setTelefono(entregaGenerada.getTelefonoEntidad());
      entrega.setOrden(entregaGenerada.getOrden());
      entregas.add(entrega);
    }

    return entregas;
  }

  private UbicacionRequest crearUbicacionRequest(UbicacionCamion ubicacionDeposito) {
    if (ubicacionDeposito == null) {
      return null;
    }

    UbicacionRequest ubicacionRequest = new UbicacionRequest();
    ubicacionRequest.setLatitud(ubicacionDeposito.getLatitud());
    ubicacionRequest.setLongitud(ubicacionDeposito.getLongitud());
    return ubicacionRequest;
  }

  private List<CamionRutaRequest> crearCamionesRequest(List<Camion> camiones) {
    List<CamionRutaRequest> camionesRequest = new ArrayList<>();

    for (Camion camion : camiones) {
      CamionRutaRequest camionRequest = new CamionRutaRequest();
      camionRequest.setPatente(camion.getPatente());
      camionRequest.setDisponible(camion.estaDisponible());
      camionesRequest.add(camionRequest);
    }

    return camionesRequest;
  }

  private List<AsignacionRutaRequest> crearAsignacionesRequest(
      List<AsignacionDisponible> asignaciones
  ) {
    List<AsignacionRutaRequest> asignacionesRequest = new ArrayList<>();

    for (AsignacionDisponible asignacion : asignaciones) {
      AsignacionRutaRequest asignacionRequest = new AsignacionRutaRequest();
      asignacionRequest.setIdDonacion(asignacion.idDonacion());
      asignacionRequest.setEstadoDonacion(asignacion.estadoDonacion());
      asignacionRequest.setRazonSocialEntidad(asignacion.razonSocial());
      asignacionRequest.setDireccionEntidad(asignacion.direccion());
      asignacionRequest.setTelefonoEntidad(asignacion.telefono());
      asignacionesRequest.add(asignacionRequest);
    }

    return asignacionesRequest;
  }

  public static class GenerarRutasRequest {
    private UbicacionRequest ubicacionDeposito;
    private List<CamionRutaRequest> camiones = new ArrayList<>();
    private List<AsignacionRutaRequest> asignaciones = new ArrayList<>();

    public UbicacionRequest getUbicacionDeposito() {
      return ubicacionDeposito;
    }

    public void setUbicacionDeposito(UbicacionRequest ubicacionDeposito) {
      this.ubicacionDeposito = ubicacionDeposito;
    }

    public List<CamionRutaRequest> getCamiones() {
      return camiones;
    }

    public void setCamiones(List<CamionRutaRequest> camiones) {
      this.camiones = camiones;
    }

    public List<AsignacionRutaRequest> getAsignaciones() {
      return asignaciones;
    }

    public void setAsignaciones(List<AsignacionRutaRequest> asignaciones) {
      this.asignaciones = asignaciones;
    }
  }

  public static class UbicacionRequest {
    private Double latitud;
    private Double longitud;

    public Double getLatitud() {
      return latitud;
    }

    public void setLatitud(Double latitud) {
      this.latitud = latitud;
    }

    public Double getLongitud() {
      return longitud;
    }

    public void setLongitud(Double longitud) {
      this.longitud = longitud;
    }
  }

  public static class CamionRutaRequest {
    private String patente;
    private boolean disponible;

    public String getPatente() {
      return patente;
    }

    public void setPatente(String patente) {
      this.patente = patente;
    }

    public boolean isDisponible() {
      return disponible;
    }

    public void setDisponible(boolean disponible) {
      this.disponible = disponible;
    }
  }

  public static class AsignacionRutaRequest {
    private String idDonacion;
    private String estadoDonacion;
    private String razonSocialEntidad;
    private String direccionEntidad;
    private String telefonoEntidad;

    public String getIdDonacion() {
      return idDonacion;
    }

    public void setIdDonacion(String idDonacion) {
      this.idDonacion = idDonacion;
    }

    public String getEstadoDonacion() {
      return estadoDonacion;
    }

    public void setEstadoDonacion(String estadoDonacion) {
      this.estadoDonacion = estadoDonacion;
    }

    public String getRazonSocialEntidad() {
      return razonSocialEntidad;
    }

    public void setRazonSocialEntidad(String razonSocialEntidad) {
      this.razonSocialEntidad = razonSocialEntidad;
    }

    public String getDireccionEntidad() {
      return direccionEntidad;
    }

    public void setDireccionEntidad(String direccionEntidad) {
      this.direccionEntidad = direccionEntidad;
    }

    public String getTelefonoEntidad() {
      return telefonoEntidad;
    }

    public void setTelefonoEntidad(String telefonoEntidad) {
      this.telefonoEntidad = telefonoEntidad;
    }
  }

  public static class RutaGeneradaResponse {
    private String patenteCamion;
    private UbicacionRequest ubicacionDeposito;
    private List<EntregaGeneradaResponse> entregas = new ArrayList<>();

    public String getPatenteCamion() {
      return patenteCamion;
    }

    public void setPatenteCamion(String patenteCamion) {
      this.patenteCamion = patenteCamion;
    }

    public UbicacionRequest getUbicacionDeposito() {
      return ubicacionDeposito;
    }

    public void setUbicacionDeposito(UbicacionRequest ubicacionDeposito) {
      this.ubicacionDeposito = ubicacionDeposito;
    }

    public List<EntregaGeneradaResponse> getEntregas() {
      return entregas;
    }

    public void setEntregas(List<EntregaGeneradaResponse> entregas) {
      this.entregas = entregas;
    }
  }

  public static class EntregaGeneradaResponse {
    private String idDonacion;
    private String razonSocialEntidad;
    private String direccionEntidad;
    private String telefonoEntidad;
    private int orden;

    public String getIdDonacion() {
      return idDonacion;
    }

    public void setIdDonacion(String idDonacion) {
      this.idDonacion = idDonacion;
    }

    public String getRazonSocialEntidad() {
      return razonSocialEntidad;
    }

    public void setRazonSocialEntidad(String razonSocialEntidad) {
      this.razonSocialEntidad = razonSocialEntidad;
    }

    public String getDireccionEntidad() {
      return direccionEntidad;
    }

    public void setDireccionEntidad(String direccionEntidad) {
      this.direccionEntidad = direccionEntidad;
    }

    public String getTelefonoEntidad() {
      return telefonoEntidad;
    }

    public void setTelefonoEntidad(String telefonoEntidad) {
      this.telefonoEntidad = telefonoEntidad;
    }

    public int getOrden() {
      return orden;
    }

    public void setOrden(int orden) {
      this.orden = orden;
    }
  }
}
