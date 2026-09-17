package org.example.integracion;

import java.util.ArrayList;
import java.util.List;
import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.Repositorios.RepositorioCamiones;
import org.example.api.logistica.dto.RutaRequest;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.UbicacionCamion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class ClienteGeneradorRutas {

  private static final String URL_GENERADOR_RUTAS = "http://localhost:8081/rutas/generar";

  private final RestTemplate restTemplate;
  private final String urlGeneradorRutas;
  private final RepositorioCamiones repositorioCamiones;
  private final RepositorioAsignacionesDonacion repositorioAsignaciones;

  public ClienteGeneradorRutas() {
    this(
        new RestTemplate(),
        URL_GENERADOR_RUTAS,
        RepositorioCamiones.getInstance(),
        RepositorioAsignacionesDonacion.getInstance()
    );
  }

  public ClienteGeneradorRutas(
      RestTemplate restTemplate,
      String urlGeneradorRutas,
      RepositorioCamiones repositorioCamiones,
      RepositorioAsignacionesDonacion repositorioAsignaciones
  ) {
    this.restTemplate = restTemplate;
    this.urlGeneradorRutas = urlGeneradorRutas;
    this.repositorioCamiones = repositorioCamiones;
    this.repositorioAsignaciones = repositorioAsignaciones;
  }

  public List<RutaRequest> solicitarRutasDesdeRepositorios(UbicacionCamion ubicacionDeposito) {
    return solicitarRutas(
        repositorioCamiones.buscarDisponibles(),
        repositorioAsignaciones.buscarTodas(),
        ubicacionDeposito
    );
  }

  public List<RutaRequest> solicitarRutas(
      List<Camion> camionesDisponibles,
      List<AsignacionDonacion> asignaciones,
      UbicacionCamion ubicacionDeposito
  ) {
    if (camionesDisponibles.isEmpty() || asignaciones.isEmpty()) {
      return new ArrayList<>();
    }

    GenerarRutasRequest request =
        armarRequest(camionesDisponibles, asignaciones, ubicacionDeposito);

    ResponseEntity<RutaGeneradaResponse[]> response = restTemplate.postForEntity(
        urlGeneradorRutas,
        request,
        RutaGeneradaResponse[].class
    );

    return convertirRespuesta(response.getBody());
  }

  public GenerarRutasRequest armarRequest(
      List<Camion> camionesDisponibles,
      List<AsignacionDonacion> asignaciones,
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
      List<AsignacionDonacion> asignaciones
  ) {
    List<AsignacionRutaRequest> asignacionesRequest = new ArrayList<>();

    for (AsignacionDonacion asignacion : asignaciones) {
      AsignacionRutaRequest asignacionRequest = new AsignacionRutaRequest();
      asignacionRequest.setIdDonacion(asignacion.getDonacion().getId());
      asignacionRequest.setEstadoDonacion(asignacion.getDonacion().getEstadoActual().name());
      asignacionRequest.setRazonSocialEntidad(asignacion.getEntidad().getRazonSocial());
      asignacionRequest.setDireccionEntidad(asignacion.getEntidad().getDireccion());
      asignacionRequest.setTelefonoEntidad(asignacion.getEntidad().getTelefono());
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
