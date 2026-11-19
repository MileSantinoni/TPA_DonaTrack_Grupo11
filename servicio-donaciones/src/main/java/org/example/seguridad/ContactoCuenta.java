package org.example.seguridad;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "contactos_cuenta")
public class ContactoCuenta {

  public enum Canal {
    MAIL,
    SMS,
    WHATSAPP
  }

  @Id
  @Column(length = 512)
  public String id;

  @Column(nullable = false, length = 320)
  public String email;

  @Column(nullable = false, length = 2)
  public String pais;

  @Column(length = 16)
  public String telefono;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public Canal canal = Canal.MAIL;

  @Column(nullable = false)
  public Instant actualizado;

  @Version
  public Long version;

  public ContactoCuenta() {
  }
}