package org.example.dominio.donacion;

import java.util.ArrayList;
import java.util.List;

public class RepositorioRegistroDonacion {

  // (Singleton)
  private static RepositorioRegistroDonacion instancia = null;

  //"base de datos" en memoria
  private List<RegistroDonacion> registros;

  //Constructor privado
  private RepositorioRegistroDonacion() {
    this.registros = new ArrayList<>();
  }

  //garantiza una única instancia
  public static RepositorioRegistroDonacion getInstance() {
    if (instancia == null) {
      instancia = new RepositorioRegistroDonacion();
    }
    return instancia;
  }

  // ==========================================
  // Métodos del Repositorio (ABM / Búsquedas)
  // ==========================================

  //guardar un nuevo registro de donación
  public void agregarRegistro(RegistroDonacion nuevoRegistro) {
    this.registros.add(nuevoRegistro);
  }

  //obtener todos los registros (accesible para todos)
  public List<RegistroDonacion> obtenerTodos() {
    return this.registros;
  }

  //buscar un registro específico por su ID
  public RegistroDonacion buscarPorId(String idBuscado) {
    for (RegistroDonacion registro : registros) {
      if (registro.getId().equals(idBuscado)) {
        return registro;
      }
    }
    return null; // O lanzar una excepción si no se encuentra
  }
}
