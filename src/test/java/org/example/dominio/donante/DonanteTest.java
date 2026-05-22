package org.example.dominio.donante;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DonanteTest {

  @Test
  void crearUnDonanteHumano() {

    PersonaHumana donante = new PersonaHumana(
        "Pedro@mail.com",
        "12345678",
        TipoDocumento.DNI,
        "Pedro",
        "Gonzales",
        25,
        Genero.MASCULINO,
        "Av. libertador 1234"
    );

    assertEquals("Pedro", donante.getNombre());
    assertEquals("Gonzales", donante.getApellido());
    assertEquals("Pedro@mail.com", donante.getMail());
    assertEquals(EstadoRegistro.PRIMER_ACCESO, donante.getEstadoRegistro());
    assertEquals(TipoContactoPredeterminado.MAIL, donante.getContactoPredeterminado());
  }

  @Test
  void gestionarUnaPersonaHumana() {

    PersonaHumana donante = new PersonaHumana(
        "sofia.ramirez@mail.com",
        "33444555",
        TipoDocumento.DNI,
        "Sofia",
        "Ramirez",
        29,
        Genero.FEMENINO,
        "Av. Corrientes 1234"
    );

    MedioContacto whatsapp = new MedioContacto(
        TipoMedioContacto.WHATSAPP,
        "1166778899"
    );

    donante.agregarMedioContacto(whatsapp);

    donante.definirContactoPredeterminado(
        TipoContactoPredeterminado.WHATSAPP
    );

    donante.activar();

    donante.actualizarDatos(
        "sol.martinez@mail.com",
        "99888777",
        TipoDocumento.DNI
    );

    donante.setNombre("Sol");
    donante.setApellido("Martinez");
    donante.setEdad(30);
    donante.setGenero(Genero.OTRO);
    donante.setDireccion("Av. Santa Fe 2500");

    assertEquals("Sol", donante.getNombre());
    assertEquals("Martinez", donante.getApellido());
    assertEquals("sol.martinez@mail.com", donante.getMail());
    assertEquals(EstadoRegistro.ACTIVO, donante.getEstadoRegistro());
    assertEquals(1, donante.getMediosDeContacto().size());
    assertEquals(TipoContactoPredeterminado.WHATSAPP, donante.getContactoPredeterminado());
  }

  @Test
  void gestionarUnaPersonaJuridica() {

    PersonaJuridica empresa = new PersonaJuridica(
        "contacto@techsolidaria.org",
        "30777888991",
        TipoDocumento.CUIT,
        "Tech Solidaria",
        TipoOrganizacion.ONG,
        "Educacion"
    );

    Representante representante1 = new Representante(
        "Lucia",
        "Martinez",
        "lucia@techsolidaria.org"
    );

    Representante representante2 = new Representante(
        "Carlos",
        "Gimenez",
        "carlos@techsolidaria.org"
    );

    MedioContacto telefono = new MedioContacto(
        TipoMedioContacto.TELEFONO,
        "1144556677"
    );

    empresa.agregarRepresentante(representante1);
    empresa.agregarRepresentante(representante2);

    empresa.agregarMedioContacto(telefono);

    empresa.definirContactoPredeterminado(
        TipoContactoPredeterminado.TELEFONO
    );

    empresa.activar();

    empresa.actualizarDatos(
        "info@techsolidaria.org",
        "30999111222",
        TipoDocumento.CUIT
    );

    empresa.setRazonSocial("Fundacion Tech Solidaria");
    empresa.setRubro("Asistencia Social");

    assertEquals("Fundacion Tech Solidaria", empresa.getRazonSocial());

    assertEquals("Asistencia Social", empresa.getRubro());

    assertEquals(EstadoRegistro.ACTIVO, empresa.getEstadoRegistro());

    assertEquals(2, empresa.getRepresentantes().size());

    assertEquals(1, empresa.getMediosDeContacto().size());

    assertEquals(TipoContactoPredeterminado.TELEFONO, empresa.getContactoPredeterminado());
  }

  @Test
  void unDonanteNoPuedeCrearseSinMail() {

    assertThrows(
        IllegalArgumentException.class,
        () -> new PersonaHumana(
            "",
            "40111222",
            TipoDocumento.DNI,
            "Valentina",
            "Sosa",
            27,
            Genero.FEMENINO,
            "Av. Belgrano 742"
        )
    );
  }

  @Test
  void unAdministradorPuedeActivarUnDonante() {
    Administrador administrador = new Administrador(
        "Laura",
        "laura.admin@donatrack.com"
    );

    PersonaHumana donante = new PersonaHumana(
        "nicolas.paz@mail.com",
        "35111222",
        TipoDocumento.DNI,
        "Nicolas",
        "Paz",
        31,
        Genero.MASCULINO,
        "Av. Nazca 1200"
    );

    administrador.activarDonante(donante);

    assertEquals(EstadoRegistro.ACTIVO, donante.getEstadoRegistro());
  }
}

