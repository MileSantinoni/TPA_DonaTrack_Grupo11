package org.example.dominio.notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.catalogo.Subcategoria;
import org.example.dominio.catalogo.TipoAtributo;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.Genero;
import org.example.dominio.donante.PersonaHumana;
import org.example.dominio.donante.TipoDocumento;
import org.example.dominio.logistica.Camion;
import org.example.dominio.logistica.Entrega;
import org.example.dominio.logistica.Ruta;
import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.Notificacion;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;
import org.junit.jupiter.api.Test;

public class NotificadorLogisticaTest {

  @Test
  public void losEventosLogisticosNotificanALaEntidadYAlDonante() {
    NotificadorConRegistro notificador = new NotificadorConRegistro();

    Entrega entrega = entregaDePrueba();
    Ruta ruta = new Ruta(new Camion("AB123CD", 20.0, 2.5, 3500.0));
    ruta.agregarEntrega(entrega);

    notificador.notificarInicioRuta(ruta);
    entrega.getDonacion().cambiarEstado(org.example.dominio.donacion.EstadoDonacion.ASIGNACION_REALIZADA, "Asignacion");
    entrega.getDonacion().cambiarEstado(org.example.dominio.donacion.EstadoDonacion.LISTA_PARA_ENTREGAR, "Planificacion");
    entrega.iniciarTraslado();
    entrega.confirmarRecepcion(ruta.getCamion(), notificador);
    notificador.notificarEntregaNoRecibida(entrega, "La entidad no pudo recibir");

    assertEquals(3, notificador.mensajesEntidad.size());
    assertEquals(3, notificador.mensajesDonante.size());
  }

  private Entrega entregaDePrueba() {
    EntidadBeneficiaria entidad = new EntidadBeneficiaria(
        "Comedor Sol",
        "Av. Siempreviva 742",
        "1140001111"
    );
    Donante donante = new PersonaHumana(
        "donante@test.com",
        "12345678",
        TipoDocumento.DNI,
        "Juan",
        "Perez",
        30,
        Genero.MASCULINO,
        "Calle 123"
    );
    Subcategoria subcategoria = new Subcategoria(
        UUID.randomUUID().toString(),
        "Alimentos",
        TipoAtributo.PERECEDERO
    );
    Donacion donacion = new Donacion(
        "Fideos",
        100,
        "Paquetes",
        subcategoria,
        null,
        null,
        donante
    );

    return new Entrega(donacion, entidad, 1);
  }

  static class NotificadorConRegistro extends Notificador {
    private final List<String> mensajesEntidad = new ArrayList<>();
    private final List<String> mensajesDonante = new ArrayList<>();

    public NotificadorConRegistro() {
      super(new Email(null), new SMS(), new WhatsApp());
    }

    @Override
    public Notificacion notificarEntidadBeneficiaria(EntidadBeneficiaria entidad, String mensaje) {
      mensajesEntidad.add(mensaje);
      Notificacion notificacion = new Notificacion(entidad.getTelefono(), mensaje);
      notificacion.marcarComoCompletada();
      return notificacion;
    }

    @Override
    public Notificacion notificarDonante(Donante donante, String mensaje) {
      mensajesDonante.add(mensaje);
      Notificacion notificacion = new Notificacion(donante.getMail(), mensaje);
      notificacion.marcarComoCompletada();
      return notificacion;
    }
  }
}
