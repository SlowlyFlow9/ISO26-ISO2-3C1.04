package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class CitasIncidenciasServiceImplTest {

    private static final long MEDICO_ID = 30L;
    private static final LocalDateTime FECHA_CITA =
            LocalDateTime.of(2026, 11, 27, 10, 0);
    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 11, 27, 17, 0);
    private static final Usuario MEDICO =
            new Usuario("medico", Rol.MEDICO, MEDICO_ID);

    private CitasRepositorioMemoria citas;
    private IncidenciasRepositorioMemoria incidencias;
    private CitasIncidenciasServiceImpl servicio;

    @BeforeEach
    void preparar() {
        citas = new CitasRepositorioMemoria();
        incidencias = new IncidenciasRepositorioMemoria();
        servicio = new CitasIncidenciasServiceImpl(
                citas,
                incidencias,
                Clock.fixed(
                        Instant.parse("2026-11-27T17:00:00Z"),
                        ZoneOffset.UTC));
    }

    private Cita guardarCita(
            long medicoId,
            EstadoCita estado) {
        Cita cita = new Cita();
        cita.setPacienteId(10L);
        cita.setMedicoId(medicoId);
        cita.setTipoPruebaId(40L);
        cita.setFechaHora(FECHA_CITA);
        cita.setEstado(estado);
        return citas.guardar(cita);
    }

    @Test
    void registraVariasIncidenciasSinSobrescribirlas()
            throws Exception {
        Cita cita = guardarCita(
                MEDICO_ID, EstadoCita.REALIZADA);

        IncidenciaCita primera =
                servicio.registrarIncidencia(
                        MEDICO, cita.getId(), "  Dolor  ");
        IncidenciaCita segunda =
                servicio.registrarIncidencia(
                        MEDICO, cita.getId(), "Reacción");
        List<IncidenciaCita> historial =
                servicio.listarIncidencias(MEDICO, cita.getId());

        assertEquals(2, historial.size());
        assertTrue(primera.getId() > 0);
        assertTrue(segunda.getId() > primera.getId());
        assertEquals("Dolor", primera.getDescripcion());
        assertEquals(AHORA, primera.getFechaHora());
        assertEquals(MEDICO_ID, primera.getMedicoId());
        assertEquals(EstadoCita.REALIZADA,
                citas.buscarPorId(cita.getId())
                        .orElseThrow().getEstado());
    }

    @Test
    void rechazaDescripcionVaciaYCitaCancelada() {
        Cita programada = guardarCita(
                MEDICO_ID, EstadoCita.PROGRAMADA);
        Cita cancelada = guardarCita(
                MEDICO_ID, EstadoCita.CANCELADA);

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.registrarIncidencia(
                        MEDICO, programada.getId(), "  "));
        assertThrows(
                CitaNoIncidenciableException.class,
                () -> servicio.registrarIncidencia(
                        MEDICO, cancelada.getId(), "Incidencia"));
        assertTrue(incidencias.listarPorCita(
                programada.getId()).isEmpty());
        assertTrue(incidencias.listarPorCita(
                cancelada.getId()).isEmpty());
    }

    @Test
    void soloElMedicoResponsablePuedeRegistrarYConsultar()
            throws Exception {
        Cita cita = guardarCita(
                MEDICO_ID, EstadoCita.PROGRAMADA);
        Usuario otroMedico =
                new Usuario("otro", Rol.MEDICO, 31L);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.registrarIncidencia(
                        otroMedico, cita.getId(), "Incidencia"));
        assertThrows(
                SinPermisoException.class,
                () -> servicio.listarIncidencias(
                        otroMedico, cita.getId()));
        assertThrows(
                SinPermisoException.class,
                () -> servicio.registrarIncidencia(
                        new Usuario("paciente", Rol.PACIENTE),
                        cita.getId(),
                        "Incidencia"));
    }

    @Test
    void rechazaCitaInexistente() {
        assertThrows(
                CitaNoEncontradaException.class,
                () -> servicio.registrarIncidencia(
                        MEDICO, 999L, "Incidencia"));
    }
}
