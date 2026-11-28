package notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cita.IdentidadPacienteMemoria;
import cita.PacienteNoVinculadoException;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class NotificacionesServiceImplTest {

    private static final Usuario PACIENTE_1 =
            new Usuario("paciente1", Rol.PACIENTE);

    private NotificacionesRepositorioMemoria repositorio;
    private IdentidadPacienteMemoria identidad;
    private NotificacionesServiceImpl servicio;

    @BeforeEach
    void preparar() {
        repositorio = new NotificacionesRepositorioMemoria();
        identidad = new IdentidadPacienteMemoria();
        identidad.vincular("paciente1", 10L);
        identidad.vincular("paciente2", 20L);
        identidad.vincular("paciente3", 30L);

        repositorio.guardar(new Notificacion(
                1L,
                10L,
                "Cita programada",
                "Tiene una cita pendiente.",
                LocalDateTime.of(2026, 11, 28, 10, 0),
                false));
        repositorio.guardar(new Notificacion(
                2L,
                10L,
                "Datos actualizados",
                "Se han actualizado sus datos.",
                LocalDateTime.of(2026, 11, 27, 9, 30),
                true));
        repositorio.guardar(new Notificacion(
                3L,
                20L,
                "Mensaje privado",
                "Notificación de otro paciente.",
                LocalDateTime.of(2026, 11, 26, 8, 0),
                false));

        servicio = new NotificacionesServiceImpl(
                repositorio,
                identidad);
    }

    @Test
    void listarDevuelveSoloNotificacionesDelPaciente() throws Exception {
        var resultado = servicio.listarMisNotificaciones(PACIENTE_1);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream()
                .allMatch(notificacion ->
                        notificacion.pacienteId() == 10L));
        assertEquals("Cita programada", resultado.get(0).titulo());
        assertEquals("Tiene una cita pendiente.",
                resultado.get(0).mensaje());
        assertEquals(
                LocalDateTime.of(2026, 11, 28, 10, 0),
                resultado.get(0).fechaHora());
        assertFalse(resultado.get(0).leida());
        assertTrue(resultado.get(1).leida());
    }

    @Test
    void pacienteSinNotificacionesRecibeListaVacia() throws Exception {
        assertTrue(servicio.listarMisNotificaciones(
                new Usuario("paciente3", Rol.PACIENTE)).isEmpty());
    }

    @Test
    void rechazaUsuarioSinRolPaciente() {
        assertThrows(SinPermisoException.class,
                () -> servicio.listarMisNotificaciones(
                        new Usuario("medico", Rol.MEDICO)));
        assertThrows(SinPermisoException.class,
                () -> servicio.listarMisNotificaciones(
                        new Usuario("admin", Rol.ADMINISTRADOR)));
    }

    @Test
    void rechazaUsuarioNulo() {
        assertThrows(SinPermisoException.class,
                () -> servicio.listarMisNotificaciones(null));
    }

    @Test
    void rechazaPacienteSinVinculacion() {
        assertThrows(PacienteNoVinculadoException.class,
                () -> servicio.listarMisNotificaciones(
                        new Usuario("desconocido", Rol.PACIENTE)));
    }
}
