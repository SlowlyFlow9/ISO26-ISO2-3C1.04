package cita;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class HistorialPruebasServiceImplTest {

    private HistorialPruebasService servicio;
    private CitasRepositorioMemoria repositorio;
    private IdentidadPacienteMemoria identidad;

    private final Usuario paciente =
            new Usuario("paciente1", Rol.PACIENTE);

    @BeforeEach
    void preparar() {

        repositorio = new CitasRepositorioMemoria();
        identidad = new IdentidadPacienteMemoria();

        identidad.vincular("paciente1", 10L);

        CitasService citasService =
                new CitasServiceImpl(repositorio, identidad);

        servicio = new HistorialPruebasServiceImpl(
                citasService);
    }

    private Cita crearCita(
            long pacienteId, EstadoCita estado) {

        Cita cita = new Cita();
        cita.setPacienteId(pacienteId);
        cita.setMedicoId(20L);
        cita.setTipoPruebaId(30L);
        cita.setFechaHora(
                LocalDateTime.of(2026, 11, 20, 10, 0));
        cita.setEstado(estado);

        return repositorio.guardar(cita);
    }

    @Test
    void devuelveSoloLasRealizadas() throws Exception {

        crearCita(10L, EstadoCita.PROGRAMADA);
        crearCita(10L, EstadoCita.CONFIRMADA);
        crearCita(10L, EstadoCita.CANCELADA);
        crearCita(10L, EstadoCita.REALIZADA);

        var historial = servicio.listarMiHistorial(paciente);

        assertEquals(1, historial.size());
        assertEquals(
                EstadoCita.REALIZADA,
                historial.get(0).getEstado());
    }

    @Test
    void noDevuelvePruebasAjenas() throws Exception {

        crearCita(10L, EstadoCita.REALIZADA);
        crearCita(20L, EstadoCita.REALIZADA);

        var historial = servicio.listarMiHistorial(paciente);

        assertEquals(1, historial.size());
        assertEquals(10L, historial.get(0).getPacienteId());
    }

    @Test
    void devuelveListaVacia() throws Exception {

        assertTrue(
                servicio.listarMiHistorial(paciente).isEmpty());
    }

    @Test
    void conservaLosDatosDeLaPrueba() throws Exception {

        Cita original = crearCita(
                10L, EstadoCita.REALIZADA);

        Cita resultado =
                servicio.listarMiHistorial(paciente).get(0);

        assertEquals(original.getId(), resultado.getId());
        assertEquals(original.getMedicoId(),
                resultado.getMedicoId());
        assertEquals(original.getTipoPruebaId(),
                resultado.getTipoPruebaId());
        assertEquals(original.getFechaHora(),
                resultado.getFechaHora());
    }

    @Test
    void devuelveUnaCopia() throws Exception {

        Cita original = crearCita(
                10L, EstadoCita.REALIZADA);

        Cita resultado =
                servicio.listarMiHistorial(paciente).get(0);

        resultado.setEstado(EstadoCita.CANCELADA);

        assertEquals(
                EstadoCita.REALIZADA,
                repositorio.buscarPorId(
                        original.getId()).orElseThrow().getEstado());
    }

    @Test
    void rechazaUsuarioSinPermiso() {

        Usuario medico =
                new Usuario("medico", Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.listarMiHistorial(medico));
    }

    @Test
    void rechazaPacienteNoVinculado() {

        Usuario desconocido =
                new Usuario("desconocido", Rol.PACIENTE);

        assertThrows(
                PacienteNoVinculadoException.class,
                () -> servicio.listarMiHistorial(desconocido));
    }

    @Test
    void rechazaUsuarioNulo() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.listarMiHistorial(null));
    }
}
