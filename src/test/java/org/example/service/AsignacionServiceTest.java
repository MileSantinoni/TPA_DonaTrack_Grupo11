package org.example.service;

import org.example.Repositorios.RepositorioAsignacionesDonacion;
import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donacion.EstadoDonacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.TipoDocumento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AsignacionServiceTest {

    private NotificacionesServiceFake notificaciones;
    private AsignacionService asignacionService;

    static class DonanteTest extends Donante {
        public DonanteTest(String mail, String nroDoc, TipoDocumento tipo) {
            super(mail, nroDoc, tipo);
        }
    }

    static class NotificacionesServiceFake extends NotificacionesService {
        int notificacionesBeneficiario = 0;
        int notificacionesDonante = 0;

        NotificacionesServiceFake() {
            super(null);
        }

        @Override
        public org.example.dominio.notificacion.Notificacion notificarDonacionAsignadaBeneficiario(AsignacionDonacion asignacion) {
            notificacionesBeneficiario++;
            return null;
        }

        @Override
        public org.example.dominio.notificacion.Notificacion notificarDonacionAsignadaDonante(AsignacionDonacion asignacion) {
            notificacionesDonante++;
            return null;
        }
    }

    @BeforeEach
    public void setUp() {
        RepositorioAsignacionesDonacion.getInstance().limpiar();
        notificaciones = new NotificacionesServiceFake();
        asignacionService = new AsignacionService(notificaciones);
    }

    private Donacion nuevaDonacion(Donante donante) {
        return new Donacion(
                "Arroz",
                10,
                "kg",
                null,
                LocalDate.now().plusDays(30),
                null,
                donante
        );
    }

    @Test
    public void confirmarAsignacionCambiaElEstadoGuardaYNotifica() {
        Donante donante = new DonanteTest("pepito@gmail.com", "12345678", TipoDocumento.DNI);
        Donacion donacion = nuevaDonacion(donante);
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor Sol", "Calle 1", "+5491100000000");

        asignacionService.confirmarAsignacion(donacion, entidad);

        assertEquals(EstadoDonacion.ASIGNACION_REALIZADA, donacion.getEstadoActual());
        assertEquals(1, RepositorioAsignacionesDonacion.getInstance().buscarTodas().size());
        assertEquals(1, notificaciones.notificacionesBeneficiario);
        assertEquals(1, notificaciones.notificacionesDonante);
    }

    @Test
    public void noSePuedeAsignarUnaDonacionQueNoEstaEnDeposito() {
        Donante donante = new DonanteTest("pepito@gmail.com", "12345678", TipoDocumento.DNI);
        Donacion donacion = nuevaDonacion(donante);
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor Sol", "Calle 1", "+5491100000000");

        // Primera asignación: pasa a ASIGNACION_REALIZADA.
        asignacionService.confirmarAsignacion(donacion, entidad);

        // Segunda asignación sobre la misma donación: ya no está EN_DEPOSITO.
        assertThrows(IllegalStateException.class, () ->
                asignacionService.confirmarAsignacion(donacion, entidad));
    }
}
