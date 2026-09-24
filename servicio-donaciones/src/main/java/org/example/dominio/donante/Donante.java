package org.example.dominio.donante;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;



@Entity
@Table(name = "donantes")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Donante {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  protected String mail;

  @Column(name = "numero_documento")
  protected String numeroDocumento;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_documento")
  protected TipoDocumento tipoDeDocumento;

  @Enumerated(EnumType.STRING)
  @Column(name = "estado_registro")
  private EstadoRegistro estadoRegistro;

  @Enumerated(EnumType.STRING)
  @Column(name = "contacto_predeterminado")
  private TipoContactoPredeterminado contactoPredeterminado;

  @OneToMany(mappedBy = "donante", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<MedioContacto> mediosDeContacto = new ArrayList<>();

  protected Donante() {}
  public Donante(String mail, String numeroDocumento, TipoDocumento tipoDeDocumento) {

    if(mail == null || mail.isBlank()) {
      throw new IllegalArgumentException(
          "El mail es obligatorio"
      );
    }

    this.mail = mail;
    this.numeroDocumento = numeroDocumento;
    this.tipoDeDocumento = tipoDeDocumento;
    this.estadoRegistro = EstadoRegistro.PRIMER_ACCESO;
    this.contactoPredeterminado = TipoContactoPredeterminado.MAIL;
    this.mediosDeContacto = new ArrayList<>();
  }

  public void activar() {
    this.estadoRegistro = EstadoRegistro.ACTIVO;
  }

  public void agregarMedioContacto(MedioContacto medio) {
    this.mediosDeContacto.add(medio);
  }

  public void actualizarDatos(String mail, String numeroDocumento, TipoDocumento tipoDeDocumento) {
    this.mail = mail;
    this.numeroDocumento = numeroDocumento;
    this.tipoDeDocumento = tipoDeDocumento;
  }

  public void definirContactoPredeterminado(TipoContactoPredeterminado contactoPredeterminado) {
    this.contactoPredeterminado = contactoPredeterminado;
  }

  public TipoContactoPredeterminado getContactoPredeterminado() {
    return contactoPredeterminado;
  }

  public Long getId() {
    return id;
  }

  public String getMail() {
    return mail;
  }

  public EstadoRegistro getEstadoRegistro() {
    return estadoRegistro;
  }

  public List<MedioContacto> getMediosDeContacto() {
    return mediosDeContacto;
  }

  public String getNumeroDocumento() {
    return numeroDocumento;
  }

  public TipoDocumento getTipoDeDocumento() {
    return tipoDeDocumento;
  }
  public void notificarAusencia(
      List<org.example.dominio.donacion.RegistroDonacion> registros,
      java.time.LocalDate hoy, org.example.dominio.notificacion.Notificador notificador) {
    registros.stream()
        .filter(registro -> id.equals(registro.getDonante().getId()))
        .map(org.example.dominio.donacion.RegistroDonacion::getFechaDeRegistro)
        .max(java.time.LocalDate::compareTo)
        .filter(fecha -> java.time.temporal.ChronoUnit.DAYS.between(fecha, hoy) == 21)
        .ifPresent(fecha -> notificador.notificarDonante(this,
            "Hola! Te extranamos. Notamos que hace mas de 20 dias no registras interaccion en nuestra plataforma. Te gustaria realizar una nueva donacion y seguir ayudando?"));
  }

}
