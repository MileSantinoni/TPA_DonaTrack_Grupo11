package org.example.service;

import org.example.dominio.beneficiario.EntidadBeneficiaria;
import org.example.dominio.beneficiario.Representante;
import org.example.dominio.donacion.AsignacionDonacion;
import org.example.dominio.donacion.Donacion;
import org.example.dominio.donante.Donante;
import org.example.dominio.donante.TipoDocumento;
import org.example.dominio.notificacion.Email;
import org.example.dominio.notificacion.EstadoNotificacion;
import org.example.dominio.notificacion.Notificacion;
import org.example.dominio.notificacion.Notificador;
import org.example.dominio.notificacion.SMS;
import org.example.dominio.notificacion.WhatsApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class NotificacionAsignacionTest {

    private Notificador notificacionesService;

    static class DonanteTest extends Donante {
        public DonanteTest(String mail, String nroDoc, TipoDocumento tipo) {
            super(mail, nroDoc, tipo);
        }
    }

    @BeforeEach
    public void setUp() {
        Notificador notificador = new Notificador(
                new Email(null) {
                    @Override
                    public Notificacion enviar(String destinatario, String mensaje) {
                        Notificacion notificacion = new Notificacion(destinatario, mensaje);
                        notificacion.marcarComoCompletada();
                        return notificacion;
                    }
                },
                new SMS() {
                    @Override
                    public Notificacion enviar(String destinatario, String mensaje) {
                        Notificacion notificacion = new Notificacion(destinatario, mensaje);
                        notificacion.marcarComoCompletada();
                        return notificacion;
                    }
                },
                new WhatsApp() {
                    @Override
                    public Notificacion enviar(String destinatario, String mensaje) {
                        Notificacion notificacion = new Notificacion(destinatario, mensaje);
                        notificacion.marcarComoCompletada();
                        return notificacion;
                    }
                }
        );

        notificacionesService = notificador;
    }

    private AsignacionDonacion asignacionCon(Donante donante, EntidadBeneficiaria entidad) {
        Donacion donacion = new Donacion(
                "Arroz",
                10,
                "kg",
                null,
                LocalDate.now().plusDays(30),
                null,
                donante
        );

        return new AsignacionDonacion(donacion, entidad, null, LocalDate.now());
    }

    @Test
    public void notificaAlDonanteAsuMailCuandoSuDonacionEsAsignada() {
        Donante donante = new DonanteTest("pepito@gmail.com", "12345678", TipoDocumento.DNI);
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor Sol", "Calle 1", "+5491100000000");

        AsignacionDonacion asignacion = asignacionCon(donante, entidad);

        Notificacion resultado = notificacionesService.notificarDonacionAsignadaDonante(asignacion);

        assertEquals("pepito@gmail.com", resultado.getDestinatario());
        assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
    }

    @Test
    public void notificaAlBeneficiarioAlEmailDelRepresentante() {
        Donante donante = new DonanteTest("pepito@gmail.com", "12345678", TipoDocumento.DNI);
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor Sol", "Calle 1", "+5491100000000");
        entidad.agregarRepresentante(new Representante("Ana", "Diaz", "ana@comedorsol.org"));

        AsignacionDonacion asignacion = asignacionCon(donante, entidad);

        Notificacion resultado = notificacionesService.notificarDonacionAsignadaBeneficiario(asignacion);

        assertEquals("ana@comedorsol.org", resultado.getDestinatario());
        assertEquals(EstadoNotificacion.COMPLETADA, resultado.getEstado());
    }

    @Test
    public void siLaEntidadNoTieneRepresentantesNoNotificaAlBeneficiario() {
        Donante donante = new DonanteTest("pepito@gmail.com", "12345678", TipoDocumento.DNI);
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor Sol", "Calle 1", "+5491100000000");

        AsignacionDonacion asignacion = asignacionCon(donante, entidad);

        Notificacion resultado = notificacionesService.notificarDonacionAsignadaBeneficiario(asignacion);

        assertNull(resultado);
    }
}