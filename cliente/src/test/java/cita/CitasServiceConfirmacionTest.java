package cita;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU02 - Pruebas de confirmacion de citas en cliente
 */

class CitasServiceConfirmacionTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);

    private CitasServiceSimulado servicio;
    private Long citaId;
    private Long citaAjenaId;

    @BeforeEach
    void preparar() {

        servicio = new CitasServiceSimulado();

        servicio.vincularPaciente("paciente1", 10L);
        servicio.vincularPaciente("paciente2", 20L);

        citaId = servicio.agregarCita(
                crearCita(10L, EstadoCita.PROGRAMADA))
                .getId();

        citaAjenaId = servicio.agregarCita(
                crearCita(20L, EstadoCita.PROGRAMADA))
                .getId();
    }

    private Cita crearCita(
            long pacienteId,
            EstadoCita estado) {

        Cita cita = new Cita();

        cita.setPacienteId(pacienteId);
        cita.setMedicoId(30L);
        cita.setTipoPruebaId(40L);
        cita.setFechaHora(
                LocalDateTime.of(2026, 12, 10, 10, 0));
        cita.setEstado(estado);

        return cita;
    }

    @Test
    void confirmarCitaProgramada() throws Exception {

        Cita confirmada =
                servicio.confirmarMiCita(PACIENTE, citaId);

        assertEquals(
                EstadoCita.CONFIRMADA,
                confirmada.getEstado());

        assertEquals(
                EstadoCita.CONFIRMADA,
                servicio.consultarMiCita(PACIENTE, citaId)
                        .getEstado());
    }

    @Test
    void confirmarConservaLosDatos() throws Exception {

        Cita confirmada =
                servicio.confirmarMiCita(PACIENTE, citaId);

        assertAll(
                () -> assertEquals(citaId, confirmada.getId()),
                () -> assertEquals(10L, confirmada.getPacienteId()),
                () -> assertEquals(30L, confirmada.getMedicoId()),
                () -> assertEquals(40L, confirmada.getTipoPruebaId()),
                () -> assertEquals(
                        LocalDateTime.of(2026, 12, 10, 10, 0),
                        confirmada.getFechaHora())
        );
    }

    @Test
    void confirmarDevuelveUnaCopia() throws Exception {

        Cita confirmada =
                servicio.confirmarMiCita(PACIENTE, citaId);

        confirmada.setEstado(EstadoCita.CANCELADA);

        assertEquals(
                EstadoCita.CONFIRMADA,
                servicio.consultarMiCita(PACIENTE, citaId)
                        .getEstado());
    }

    @Test
    void rechazarSegundaConfirmacion() throws Exception {

        servicio.confirmarMiCita(PACIENTE, citaId);

        assertThrows(
                CitaNoConfirmableException.class,
                () -> servicio.confirmarMiCita(PACIENTE, citaId));
    }

    @Test
    void rechazarCitaCancelada() {

        Long id = servicio.agregarCita(
                crearCita(10L, EstadoCita.CANCELADA))
                .getId();

        assertThrows(
                CitaNoConfirmableException.class,
                () -> servicio.confirmarMiCita(PACIENTE, id));
    }

    @Test
    void rechazarCitaRealizada() {

        Long id = servicio.agregarCita(
                crearCita(10L, EstadoCita.REALIZADA))
                .getId();

        assertThrows(
                CitaNoConfirmableException.class,
                () -> servicio.confirmarMiCita(PACIENTE, id));
    }

    @Test
    void rechazarCitaAjena() {

        assertThrows(
                CitaNoEncontradaException.class,
                () -> servicio.confirmarMiCita(
                        PACIENTE, citaAjenaId));
    }

    @Test
    void rechazarCitaInexistente() {

        assertThrows(
                CitaNoEncontradaException.class,
                () -> servicio.confirmarMiCita(
                        PACIENTE, 999L));
    }

    @Test
    void rechazarMedicoSinPermiso() {

        Usuario medico = new Usuario("medico", Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.confirmarMiCita(
                        medico, citaId));
    }

    @Test
    void rechazarPacienteSinVinculacion() {

        Usuario desconocido =
                new Usuario("desconocido", Rol.PACIENTE);

        assertThrows(
                PacienteNoVinculadoException.class,
                () -> servicio.confirmarMiCita(
                        desconocido, citaId));
    }

    @Test
    void rechazarUsuarioNulo() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.confirmarMiCita(
                        null, citaId));
    }
}
