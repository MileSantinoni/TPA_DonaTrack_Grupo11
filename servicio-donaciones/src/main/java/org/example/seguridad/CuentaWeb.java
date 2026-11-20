package org.example.seguridad;

import javax.persistence.*;
import org.example.controllers.LogtoController.Usuario;
import org.example.dominio.donante.Donante;
import org.example.dominio.beneficiario.EntidadBeneficiaria;

@Entity
@Table(name = "cuentas_web")
public class CuentaWeb {

  @Id
  @Column(length = 512)
  public String id;

  @Column(nullable = false)
  public String rol;

  @Column(nullable = false)
  public String nombre;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "donante_id", unique = true)
  public Donante donante;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "entidad_id", unique = true)
  public EntidadBeneficiaria entidad;

  // Se guarda en UTC, independientemente de la zona del servidor.
  @Column(name = "ultima_interaccion_utc", nullable = false)
  public java.time.LocalDateTime ultimaInteraccionUtc =
      java.time.LocalDateTime.now(java.time.ZoneOffset.UTC);

  @Column(name = "aviso_ausencia_generado", nullable = false)
  public boolean avisoAusenciaGenerado;

  public CuentaWeb() {
  }

  public boolean completa() {
    return "DONANTE".equals(rol) && donante != null
        || "ENTIDAD".equals(rol) && entidad != null;
  }

  public static String clave(Usuario usuario) {
    return usuario.issuer() + "|" + usuario.sub();
  }
}