package cita;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU02 - Pruebas de confirmación de citas.
 */

class CitasServiceConfirmacionTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);

    private CitasRepositorioMemoria repositorio;
    private IdentidadPacienteMemoria identidad;
    private CitasServiceImpl servicio;

    private Long citaId;

    @BeforeEach
    void preparar() {

        repositorio = new CitasRepositorioMemoria();
        identidad = new IdentidadPacienteMemoria();

        identidad.vincular("paciente1", 10L);
        identidad.vincular("paciente2", 20L);

        servicio = ServiciosCitasPrueba.citas(
                repositorio, identidad);

        citaId = repositorio.guardar(
                nuevaCita(10L, EstadoCita.PROGRAMADA))
                .getId();
    }

    private Cita nuevaCita(
            long pacienteId,
            EstadoCita estado) {

        Cita cita = new Cita();

        cita.setPacienteId(pacienteId);
        cita.setMedicoId(30L);
        cita.setTipoPruebaId(40L);

        cita.setFechaHora(
                LocalDateTime.of(
                        2026, 12, 10, 10, 0));

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

        //el cambio debe quedar guardado
        assertEquals(
                EstadoCita.CONFIRMADA,
                repositorio.buscarPorId(citaId)
                        .orElseThrow()
                        .getEstado());
    }

    @Test
    void confirmarConservaLosDatos() throws Exception {

        Cita confirmada =
                servicio.confirmarMiCita(PACIENTE, citaId);

        assertAll(
                () -> assertEquals(
                        citaId, confirmada.getId()),

                () -> assertEquals(
                        10L, confirmada.getPacienteId()),

                () -> assertEquals(
                        30L, confirmada.getMedicoId()),

                () -> assertEquals(
                        40L, confirmada.getTipoPruebaId()),

                () -> assertEquals(
                        LocalDateTime.of(
                                2026, 12, 10, 10, 0),
                        confirmada.getFechaHora())
        );
    }

    @Test
    void rechazarCitaYaConfirmada() throws Exception {

        servicio.confirmarMiCita(PACIENTE, citaId);

        assertThrows(
                CitaNoConfirmableException.class,
                () -> servicio.confirmarMiCita(
                        PACIENTE, citaId));
    }

    @Test
    void rechazarCitaCancelada() {

        Cita cita =
                nuevaCita(10L, EstadoCita.CANCELADA);

        Long id = repositorio.guardar(cita).getId();

        assertThrows(
                CitaNoConfirmableException.class,
                () -> servicio.confirmarMiCita(
                        PACIENTE, id));

        assertEquals(
                EstadoCita.CANCELADA,
                repositorio.buscarPorId(id)
                        .orElseThrow().getEstado());
    }

    @Test
    void rechazarCitaRealizada() {

        Cita cita =
                nuevaCita(10L, EstadoCita.REALIZADA);

        Long id = repositorio.guardar(cita).getId();

        assertThrows(
                CitaNoConfirmableException.class,
                () -> servicio.confirmarMiCita(
                        PACIENTE, id));
    }

    @Test
    void rechazarCitaDeOtroPaciente() {

        Usuario otroPaciente =
                new Usuario("paciente2", Rol.PACIENTE);

        assertThrows(
                CitaNoEncontradaException.class,
                () -> servicio.confirmarMiCita(
                        otroPaciente, citaId));

        // La cita original sigue programada.
        assertEquals(
                EstadoCita.PROGRAMADA,
                repositorio.buscarPorId(citaId)
                        .orElseThrow().getEstado());
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

        Usuario medico =
                new Usuario("medico", Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.confirmarMiCita(
                        medico, citaId));
    }

    @Test
    void rechazarUsuarioNulo() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.confirmarMiCita(
                        null, citaId));
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
}
