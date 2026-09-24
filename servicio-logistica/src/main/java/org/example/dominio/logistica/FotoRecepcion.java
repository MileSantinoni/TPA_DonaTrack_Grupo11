package org.example.dominio.logistica;

import javax.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "fotos_recepcion")
public class FotoRecepcion {

  @Id
  private String id = UUID.randomUUID().toString();

  @Column(nullable = false, length = 2048)
  private String url;

  protected FotoRecepcion() {
  }

  public FotoRecepcion(String url) {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("La foto es obligatoria");
    }
    this.url = url;
  }

  public String getId() {
    return id;
  }

  public String getUrl() {
    return url;
  }
}