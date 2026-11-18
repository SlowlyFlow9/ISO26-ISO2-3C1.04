package solicitud;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Paciente;
import paciente.PacientesRepositorioMemoria;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

import tipoprueba.RepositorioTiposPruebaMemoria;
import tipoprueba.TipoPrueba;

/**
 * Pruebas del servicio de prescripcion de pruebas.
 * CU11 - Servidor.
 */
public class SolicitudesServiceTest {

    private PacientesRepositorioMemoria pacientes;
    private RepositorioTiposPruebaMemoria tipos;
    private SolicitudesRepositorioMemoria solicitudes;
    private SolicitudesServiceImpl servicio;

    private long pacienteId;
    private long tipoId;

    //Prepara los datos antes de cada prueba
    @BeforeEach //lo ejecuta antes de cada prueba
    public void preparar() {

        pacientes = new PacientesRepositorioMemoria();
        tipos = new RepositorioTiposPruebaMemoria();
        solicitudes = new SolicitudesRepositorioMemoria();

        servicio = new SolicitudesServiceImpl(
                pacientes, tipos, solicitudes);

        pacienteId = pacientes.guardar(
                new Paciente()).getId();

        tipoId = tipos.guardar(new TipoPrueba(
                0, "Radiografia", "Prueba diagnostica",
                15, false, "", true)).getId();
    }

    //Crea una solicitud con los datos indicados
    private SolicitudPrueba datos(
            long paciente, long tipo, String indicacion,
            String justificacion, Prioridad prioridad) {

        return new SolicitudPrueba(
                0, paciente, 2, tipo,
                indicacion, justificacion, prioridad,
                LocalDateTime.of(2020, 1, 1, 0, 0),
                EstadoSolicitud.PENDIENTE_CITACION
        );
    }

    //Datos correctos para las pruebas
    private SolicitudPrueba datosValidos() {
        return datos(
                pacienteId, tipoId,
                "Dolor de rodilla",
                "Solicitar resonancia",
                Prioridad.PREFERENTE
        );
    }

    //Usuario con rol medico
    private Usuario medico() {
        return new Usuario("Medico de prueba", Rol.MEDICO);
    }

    @Test
    public void registrarSolicitudValida()
            throws SinPermisoException {

        SolicitudPrueba creada =
                servicio.crearSolicitud(medico(), datosValidos());

        assertTrue(creada.getId() > 0);
        assertEquals(Prioridad.PREFERENTE, creada.getPrioridad());

        assertEquals(
                EstadoSolicitud.PENDIENTE_CITACION,
                creada.getEstado());

        assertTrue(creada.getFechaSolicitud().isAfter(
                LocalDateTime.of(2020, 1, 1, 0, 0)));

        assertTrue(servicio.consultarSolicitud(
                creada.getId()).isPresent());
    }

    @Test
    public void rechazarUsuarioSinPermiso() {

        Usuario administrativo = new Usuario(
                "Administrativo", Rol.PERSONAL_ADMINISTRATIVO);

        assertThrows(SinPermisoException.class,
                () -> servicio.crearSolicitud(
                        administrativo, datosValidos()));
    }

    @Test
    public void rechazarDatosVacios() {

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearSolicitud(medico(),
                        datos(pacienteId, tipoId, " ",
                                "Justificacion", Prioridad.URGENTE)));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearSolicitud(medico(),
                        datos(pacienteId, tipoId, "Motivo",
                                " ", Prioridad.PREFERENTE)));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearSolicitud(medico(),
                        datos(pacienteId, tipoId, "Motivo",
                                "Justificacion", null)));

        assertTrue(servicio.consultarSolicitud(1).isEmpty());
    }

    @Test
    public void rechazarPacienteInexistente() {

        assertThrows(NoSuchElementException.class,
                () -> servicio.crearSolicitud(medico(),
                        datos(999, tipoId, "Motivo",
                                "Justificacion", Prioridad.ORDINARIA)));
    }

    @Test
    public void rechazarPacienteInactivo() {

        Paciente paciente =
                pacientes.buscarPorId(pacienteId).orElseThrow();

        paciente.setActivo(false);

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearSolicitud(
                        medico(), datosValidos()));
    }

    @Test
    public void rechazarTipoInexistente() {

        assertThrows(NoSuchElementException.class,
                () -> servicio.crearSolicitud(medico(),
                        datos(pacienteId, 999, "Motivo",
                                "Justificacion", Prioridad.PREFERENTE)));
    }

    @Test
    public void rechazarTipoInactivo() {

        TipoPrueba tipo =
                tipos.buscarPorId(tipoId).orElseThrow();

        tipos.guardar(tipo.conActivo(false));

        assertThrows(IllegalArgumentException.class,
                () -> servicio.crearSolicitud(
                        medico(), datosValidos()));
    }

    @Test
    public void listarSoloTiposActivos() {

        tipos.guardar(new TipoPrueba(
                0, "Prueba desactivada",
                "No disponible", 20, false, "", false));

        assertEquals(1,
                servicio.consultarTiposPrueba().size());
    }

    @Test
    public void consultarSolicitudInexistente() {

        assertTrue(
                servicio.consultarSolicitud(999).isEmpty());
    }
}
