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

  public CuentaWeb() {
  }

  public static String clave(Usuario usuario) {
    return usuario.issuer() + "|" + usuario.sub();
  }
}