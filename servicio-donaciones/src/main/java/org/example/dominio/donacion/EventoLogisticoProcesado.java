package org.example.dominio.donacion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.persistence.*;

@Entity
@Table(name = "eventos_logisticos_procesados")
public class EventoLogisticoProcesado {

  private static final ObjectMapper JSON =
      new ObjectMapper().findAndRegisterModules();

  @Id
  @Column(name = "id_operacion", nullable = false)
  private String idOperacion;


  @javax.persistence.Column(
      name = "contenido",
      nullable = false,
      length = 100000
  )
  private String contenido;

  protected EventoLogisticoProcesado() {
  }

  public EventoLogisticoProcesado(EventoLogistico evento) {
    this.idOperacion = evento.idOperacion();

    try {
      this.contenido = JSON.writeValueAsString(evento);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(
          "No se pudo serializar el evento logistico", e
      );
    }
  }

  public boolean correspondeA(EventoLogistico evento) {
    try {
      EventoLogistico guardado =
          JSON.readValue(contenido, EventoLogistico.class);

      return guardado.equals(evento);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "No se pudo leer el evento logistico guardado", e
      );
    }
  }
}