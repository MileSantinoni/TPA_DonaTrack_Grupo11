package org.example.Repositorios;

import java.util.ArrayList;
import java.util.List;
import org.example.dominio.donacion.RegistroDonacion;

public class RepositorioRegistroDonacion {

  // (Singleton)
  private static RepositorioRegistroDonacion instancia = null;
  private List<RegistroDonacion> registros;


  private RepositorioRegistroDonacion() {
    this.registros = new ArrayList<>();
  }


  public static RepositorioRegistroDonacion getInstance() {
    if (instancia == null) {
      instancia = new RepositorioRegistroDonacion();
    }
    return instancia;
  }




  public void agregarRegistro(RegistroDonacion nuevoRegistro) {
    this.registros.add(nuevoRegistro);
  }


  public List<RegistroDonacion> obtenerTodos() {
    return this.registros;
  }


  public RegistroDonacion buscarPorId(String idBuscado) {
    for (RegistroDonacion registro : registros) {
      if (registro.getId().equals(idBuscado)) {
        return registro;
      }
    }
    return null;
  }

  // Metodo solo para usar en los tests @BeforeEach
  public void limpiarRepositorio() {
    this.registros.clear();
  }
}
