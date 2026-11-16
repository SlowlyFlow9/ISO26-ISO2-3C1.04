package agenda;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class AgendasServiceSimuladoTest {

    private static final Usuario PERSONAL =
            new Usuario(
                    "personal",
                    Rol.PERSONAL_ADMINISTRATIVO);

    private AgendasServiceSimulado servicio;

    @BeforeEach
    void setUp() {
        servicio =
                new AgendasServiceSimulado();
    }

    private AgendaMedica valida() {

        AgendaMedica agenda =
                new AgendaMedica();

        agenda.setMedicoId(1L);

        agenda.setFecha(
                LocalDate.of(2026, 11, 17));

        agenda.setHoraInicio(
                LocalTime.of(8, 0));

        agenda.setHoraFin(
                LocalTime.of(15, 0));

        agenda.setObservaciones(
                "Consulta general");

        return agenda;
    }

    @Test
    @DisplayName(
            "Crear una agenda válida asigna ID y estado activo")
    void crearAgenda() throws Exception {

        AgendaMedica creada =
                servicio.crear(
                        PERSONAL,
                        valida());

        assertAll(
                () -> assertNotNull(
                        creada.getId()),
                () -> assertTrue(
                        creada.isActivo()),
                () -> assertEquals(
                        1L,
                        creada.getMedicoId())
        );
    }

    @Test
    @DisplayName(
            "Crear una agenda inválida genera un error")
    void crearAgendaInvalida() {

        AgendaMedica agenda = valida();
        agenda.setFecha(null);

        assertThrows(
                DatosAgendaInvalidosException.class,
                () -> servicio.crear(
                        PERSONAL,
                        agenda));
    }

    @Test
    @DisplayName(
            "Consultar devuelve la agenda creada")
    void consultarAgenda()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(
                        PERSONAL,
                        valida());

        AgendaMedica consultada =
                servicio.consultar(
                        PERSONAL,
                        creada.getId());

        assertEquals(
                creada.getId(),
                consultada.getId());
    }

    @Test
    @DisplayName(
            "Consultar una agenda inexistente genera un error")
    void consultarInexistente() {

        assertThrows(
                AgendaNoEncontradaException.class,
                () -> servicio.consultar(
                        PERSONAL,
                        999L));
    }

    @Test
    @DisplayName(
            "Modificar actualiza la agenda")
    void modificarAgenda()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(
                        PERSONAL,
                        valida());

        AgendaMedica nuevosDatos =
                valida();

        nuevosDatos.setHoraInicio(
                LocalTime.of(9, 0));

        nuevosDatos.setHoraFin(
                LocalTime.of(14, 0));

        AgendaMedica modificada =
                servicio.modificar(
                        PERSONAL,
                        creada.getId(),
                        nuevosDatos);

        assertAll(
                () -> assertEquals(
                        LocalTime.of(9, 0),
                        modificada.getHoraInicio()),
                () -> assertEquals(
                        LocalTime.of(14, 0),
                        modificada.getHoraFin())
        );
    }

    @Test
    @DisplayName(
            "Modificar con datos inválidos genera un error")
    void modificarConDatosInvalidos()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(
                        PERSONAL,
                        valida());

        AgendaMedica datos = valida();

        datos.setHoraFin(
                LocalTime.of(7, 0));

        assertThrows(
                DatosAgendaInvalidosException.class,
                () -> servicio.modificar(
                        PERSONAL,
                        creada.getId(),
                        datos));
    }

    @Test
    @DisplayName(
            "Modificar una agenda inexistente genera un error")
    void modificarInexistente() {

        assertThrows(
                AgendaNoEncontradaException.class,
                () -> servicio.modificar(
                        PERSONAL,
                        999L,
                        valida()));
    }

    @Test
    @DisplayName(
            "Eliminar hace una baja lógica")
    void eliminarAgenda()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(
                        PERSONAL,
                        valida());

        servicio.eliminar(
                PERSONAL,
                creada.getId());

        AgendaMedica consultada =
                servicio.consultar(
                        PERSONAL,
                        creada.getId());

        assertFalse(
                consultada.isActivo());
    }

    @Test
    @DisplayName(
            "Eliminar una agenda inexistente genera un error")
    void eliminarInexistente() {

        assertThrows(
                AgendaNoEncontradaException.class,
                () -> servicio.eliminar(
                        PERSONAL,
                        999L));
    }

    @Test
    @DisplayName(
            "Listar permite filtrar por médico")
    void listarPorMedico()
            throws Exception {

        AgendaMedica primera = valida();

        AgendaMedica segunda = valida();
        segunda.setMedicoId(2L);

        servicio.crear(
                PERSONAL,
                primera);

        servicio.crear(
                PERSONAL,
                segunda);

        var resultado =
                servicio.listar(
                        PERSONAL,
                        1L,
                        null,
                        null);

        assertAll(
                () -> assertEquals(
                        1,
                        resultado.size()),
                () -> assertEquals(
                        1L,
                        resultado.get(0)
                                .getMedicoId())
        );
    }

    @Test
    @DisplayName(
            "Listar permite filtrar por fecha")
    void listarPorFecha()
            throws Exception {

        servicio.crear(
                PERSONAL,
                valida());

        AgendaMedica segunda = valida();

        segunda.setFecha(
                LocalDate.of(
                        2026,
                        11,
                        18));

        servicio.crear(
                PERSONAL,
                segunda);

        var resultado =
                servicio.listar(
                        PERSONAL,
                        null,
                        LocalDate.of(
                                2026,
                                11,
                                17),
                        null);

        assertEquals(
                1,
                resultado.size());
    }

    @Test
    @DisplayName(
            "Listar permite filtrar por estado")
    void listarPorEstado()
            throws Exception {

        AgendaMedica primera =
                servicio.crear(
                        PERSONAL,
                        valida());

        AgendaMedica segunda = valida();
        segunda.setMedicoId(2L);

        servicio.crear(
                PERSONAL,
                segunda);

        servicio.eliminar(
                PERSONAL,
                primera.getId());

        assertAll(
                () -> assertEquals(
                        1,
                        servicio.listar(
                                PERSONAL,
                                null,
                                null,
                                true)
                                .size()),
                () -> assertEquals(
                        1,
                        servicio.listar(
                                PERSONAL,
                                null,
                                null,
                                false)
                                .size())
        );
    }

    @Test
    @DisplayName(
            "Un médico no puede crear agendas")
    void medicoNoPuedeCrear() {

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(
                        medico,
                        valida()));
    }

    @Test
    @DisplayName(
            "Un paciente no puede crear agendas")
    void pacienteNoPuedeCrear() {

        Usuario paciente =
                new Usuario(
                        "paciente",
                        Rol.PACIENTE);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(
                        paciente,
                        valida()));
    }

    @Test
    @DisplayName(
            "Un administrador no puede crear agendas")
    void administradorNoPuedeCrear() {

        Usuario administrador =
                new Usuario(
                        "admin",
                        Rol.ADMINISTRADOR);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(
                        administrador,
                        valida()));
    }

    @Test
    @DisplayName(
            "Un usuario nulo no puede crear agendas")
    void usuarioNuloNoPuedeCrear() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(
                        null,
                        valida()));
    }

    @Test
    @DisplayName(
            "Un médico no puede consultar agendas")
    void medicoNoPuedeConsultar()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(
                        PERSONAL,
                        valida());

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.consultar(
                        medico,
                        creada.getId()));
    }

    @Test
    @DisplayName(
            "Un médico no puede modificar agendas")
    void medicoNoPuedeModificar()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(
                        PERSONAL,
                        valida());

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.modificar(
                        medico,
                        creada.getId(),
                        valida()));
    }

    @Test
    @DisplayName(
            "Un médico no puede eliminar agendas")
    void medicoNoPuedeEliminar()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(
                        PERSONAL,
                        valida());

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.eliminar(
                        medico,
                        creada.getId()));
    }

    @Test
    @DisplayName(
            "Un médico no puede listar agendas")
    void medicoNoPuedeListar() {

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.listar(
                        medico,
                        null,
                        null,
                        null));
    }
}