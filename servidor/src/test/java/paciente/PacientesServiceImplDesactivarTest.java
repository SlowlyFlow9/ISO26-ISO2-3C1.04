package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;
import solicitud.SolicitudesRepositorioMemoria;
import solicitud.SolicitudesServiceImpl;
import tipoprueba.RepositorioTiposPruebaMemoria;
import tipoprueba.TipoPrueba;

class PacientesServiceImplDesactivarTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

    private PacientesRepositorioMemoria repositorio;
    private PacientesServiceImpl servicio;

    @BeforeEach
    void preparar() {
        repositorio = new PacientesRepositorioMemoria();
        servicio = new PacientesServiceImpl(repositorio);
    }

    private Paciente altaPaciente() throws Exception {
        Paciente paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellidos("Pérez");
        paciente.setDni("12345678Z");
        paciente.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        paciente.setTelefono("600111222");
        return servicio.crear(ADMIN, paciente);
    }

    @Test
    void desactivarConservaElPacienteYSusDatosYLoExcluyeDeLaBusqueda() throws Exception {
        Paciente creado = altaPaciente();

        servicio.desactivar(ADMIN, creado.getId());

        Paciente guardado = repositorio.buscarPorId(creado.getId()).orElseThrow();
        assertFalse(guardado.isActivo());
        assertEquals(creado.getDni(), guardado.getDni());
        assertEquals(creado.getFechaAlta(), guardado.getFechaAlta());
        assertTrue(repositorio.buscar("Ana").isEmpty());
    }

    @Test
    void conservaElHistorialDePruebasYRechazaNuevasSolicitudes() throws Exception {
        Paciente paciente = altaPaciente();
        RepositorioTiposPruebaMemoria tipos = new RepositorioTiposPruebaMemoria();
        long tipoId = tipos.guardar(new TipoPrueba(0, "Radiografia", "Prueba diagnostica", 15, false, "", true))
                .getId();
        SolicitudesRepositorioMemoria solicitudes = new SolicitudesRepositorioMemoria();
        SolicitudesServiceImpl servicioSolicitudes = new SolicitudesServiceImpl(repositorio, tipos, solicitudes);
        Usuario medico = new Usuario("medico", Rol.MEDICO);
        SolicitudPrueba solicitud = servicioSolicitudes.crearSolicitud(medico,
                new SolicitudPrueba(0, paciente.getId(), 1, tipoId, "Dolor", "Exploracion", Prioridad.ORDINARIA,
                        LocalDateTime.now(), EstadoSolicitud.PENDIENTE_CITACION));

        servicio.desactivar(ADMIN, paciente.getId());

        assertTrue(servicioSolicitudes.consultarSolicitud(solicitud.getId()).isPresent());
        assertThrows(IllegalArgumentException.class,
                () -> servicioSolicitudes.crearSolicitud(medico,
                        new SolicitudPrueba(0, paciente.getId(), 1, tipoId, "Dolor", "Exploracion",
                                Prioridad.ORDINARIA, LocalDateTime.now(), EstadoSolicitud.PENDIENTE_CITACION)));
    }

    @Test
    void pacienteInexistenteLanzaNoEncontrado() {
        assertThrows(PacienteNoEncontradoException.class, () -> servicio.desactivar(ADMIN, 99L));
    }

    @Test
    void pacienteYaInactivoLanzaExcepcion() throws Exception {
        Paciente creado = altaPaciente();
        servicio.desactivar(ADMIN, creado.getId());

        assertThrows(PacienteYaInactivoException.class, () -> servicio.desactivar(ADMIN, creado.getId()));
    }

    @Test
    void soloElPersonalAdministrativoPuedeDarDeBaja() throws Exception {
        Paciente creado = altaPaciente();

        for (Rol rol : Rol.values()) {
            if (rol == Rol.PERSONAL_ADMINISTRATIVO) continue;
            assertThrows(SinPermisoException.class,
                    () -> servicio.desactivar(new Usuario("x", rol), creado.getId()), rol.name());
        }
        assertThrows(SinPermisoException.class, () -> servicio.desactivar(null, creado.getId()));
        assertTrue(repositorio.buscarPorId(creado.getId()).orElseThrow().isActivo());
    }
}
