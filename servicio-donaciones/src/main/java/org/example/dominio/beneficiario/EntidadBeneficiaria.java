package org.example.dominio.beneficiario;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EntidadBeneficiaria {
  private String id;
  private String razonSocial;
  private String direccion;


  private String telefono;
  private List<Representante> representantes;
  private List<Necesidad> necesidades;

  public EntidadBeneficiaria(String razonSocial, String direccion, String telefono) {
    this.id = UUID.randomUUID().toString();
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = telefono;
    this.representantes = new ArrayList<>();
    this.necesidades = new ArrayList<>();
  }

  public void agregarRepresentante(Representante representante) {
    this.representantes.add(representante);
  }

  public void registrarNecesidad(Necesidad necesidad) {
    this.necesidades.add(necesidad);
  }

  //getters y settersssss -.-
  public String getId() { return id; }
  public String getRazonSocial() { return razonSocial; }
  public String getDireccion() { return direccion; }
  public String getTelefono() { return telefono; }
  public List<Representante> getRepresentantes() { return representantes; }
  public List<Necesidad> getNecesidades() { return necesidades; }


  public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }
  public void setDireccion(String direccion) { this.direccion = direccion; }
  public void setTelefono(String telefono) { this.telefono = telefono; }


}
