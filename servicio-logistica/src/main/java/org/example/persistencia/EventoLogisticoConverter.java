package org.example.persistencia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import org.example.dominio.logistica.EventoLogistico;

@Converter
public class EventoLogisticoConverter
    implements AttributeConverter<EventoLogistico, String> {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  @Override
  public String convertToDatabaseColumn(EventoLogistico evento) {
    if (evento == null) {
      return null;
    }

    try {
      return MAPPER.writeValueAsString(evento);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "No se pudo serializar el evento logistico", e
      );
    }
  }

  @Override
  public EventoLogistico convertToEntityAttribute(String json) {
    if (json == null) {
      return null;
    }

    try {
      return MAPPER.readValue(json, EventoLogistico.class);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "No se pudo reconstruir el evento logistico", e
      );
    }
  }
}