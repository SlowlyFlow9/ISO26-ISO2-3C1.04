package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

/** CU04: reglas de la cancelación en el servicio simulado del cliente (D-038). */
class CitasServiceCancelacionTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);

    private static final LocalDateTime CITA =
            LocalDateTime.of(2026, 12, 10, 10, 0);
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private CitasServiceSimulado servicio;

    @BeforeEach
    void preparar() {

        servicio = new CitasServiceSimulado();
        servicio.vincularPaciente("paciente1", 10L);
        servicio.vincularPaciente("paciente2", 20L);

        servicio.agregarCita(crearCita(10L, EstadoCita.PROGRAMADA));
        servicio.agregarCita(crearCita(10L, EstadoCita.CONFIRMADA));
        servicio.agregarCita(crearCita(10L, EstadoCita.CANCELADA));
        servicio.agregarCita(crearCita(10L, EstadoCita.REALIZADA));
        servicio.agregarCita(crearCita(20L, EstadoCita.PROGRAMADA));

        horasAntesDeLaCita(48);
    }

    private Cita crearCita(long pacienteId, EstadoCita estado) {

        Cita cita = new Cita();
        cita.setPacienteId(pacienteId);
        cita.setMedicoId(30L);
        cita.setTipoPruebaId(40L);
        cita.setFechaHora(CITA);
        cita.setEstado(estado);

        return cita;
    }

    private void horasAntesDeLaCita(long horas) {

        servicio.fijarReloj(Clock.fixed(
                CITA.minusHours(horas).atZone(ZONA).toInstant(),
                ZONA));
    }

    @Test
    void cancelarCitaProgramada() throws Exception {

        Cita cancelada = servicio.cancelarMiCita(PACIENTE, 1L);

        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
        assertEquals(EstadoCita.CANCELADA,
                servicio.consultarMiCita(PACIENTE, 1L).getEstado());
    }

    @Test
    void cancelarCitaConfirmada() throws Exception {

        Cita cancelada = servicio.cancelarMiCita(PACIENTE, 2L);

        assertEquals(EstadoCita.CANCELADA, cancelada.getEstado());
    }

    @Test
    void cancelarConservaLosDatosDeLaCita() throws Exception {

        Cita cancelada = servicio.cancelarMiCita(PACIENTE, 1L);

        assertEquals(1L, cancelada.getId());
        assertEquals(10L, cancelada.getPacienteId());
        assertEquals(30L, cancelada.getMedicoId());
        assertEquals(40L, cancelada.getTipoPruebaId());
        assertEquals(CITA, cancelada.getFechaHora());
    }

    @Test
    void rechazarCitaYaCancelada() {

        assertThrows(CitaNoModificableException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 3L));
    }

    @Test
    void rechazarCitaRealizada() {

        assertThrows(CitaNoModificableException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 4L));
    }

    @Test
    void rechazarCancelacionFueraDePlazo() throws Exception {

        horasAntesDeLaCita(10);

        assertThrows(CancelacionFueraDePlazoException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 1L));

        assertEquals(EstadoCita.PROGRAMADA,
                servicio.consultarMiCita(PACIENTE, 1L).getEstado());
    }

    @Test
    void permitirCancelarJustoEnElLimite() throws Exception {

        horasAntesDeLaCita(24);

        assertEquals(EstadoCita.CANCELADA,
                servicio.cancelarMiCita(PACIENTE, 1L).getEstado());
    }

    @Test
    void elPlazoSePuedeCambiar() throws Exception {

        servicio.fijarCancelacionMinimaHoras(0);
        horasAntesDeLaCita(1);

        assertEquals(EstadoCita.CANCELADA,
                servicio.cancelarMiCita(PACIENTE, 1L).getEstado());
    }

    @Test
    void citaCerradaInformaDelEstadoAunqueEsteFueraDePlazo() {

        horasAntesDeLaCita(1);

        assertThrows(CitaNoModificableException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 3L));
    }

    @Test
    void rechazarCitaDeOtroPaciente() {

        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 5L));
    }

    @Test
    void rechazarCitaInexistente() {

        assertThrows(CitaNoEncontradaException.class,
                () -> servicio.cancelarMiCita(PACIENTE, 999L));
    }

    @Test
    void rechazarUsuarioSinRolPaciente() {

        assertThrows(SinPermisoException.class,
                () -> servicio.cancelarMiCita(
                        new Usuario("medico1", Rol.MEDICO), 1L));
    }

    @Test
    void rechazarPacienteSinVinculacion() {

        assertThrows(PacienteNoVinculadoException.class,
                () -> servicio.cancelarMiCita(
                        new Usuario("desconocido", Rol.PACIENTE), 1L));
    }
}
