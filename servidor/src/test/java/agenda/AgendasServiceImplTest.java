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

class AgendasServiceImplTest {

    private static final Usuario PERSONAL =
            new Usuario(
                    "personal",
                    Rol.PERSONAL_ADMINISTRATIVO);

    private AgendasServiceImpl servicio;

    @BeforeEach
    void setUp() {
        AgendasRepositorio repositorio =
                new AgendasRepositorioMemoria();

        servicio = new AgendasServiceImpl(
                repositorio);
    }

    private AgendaMedica valida() {
        AgendaMedica agenda = new AgendaMedica();

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
            "Crear una agenda válida asigna identificador y la deja activa")
    void crearAgendaCorrectamente()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(PERSONAL, valida());

        assertAll(
                () -> assertNotNull(creada.getId()),
                () -> assertTrue(creada.isActivo()),
                () -> assertEquals(
                        1L,
                        creada.getMedicoId()),
                () -> assertEquals(
                        LocalDate.of(2026, 11, 17),
                        creada.getFecha()),
                () -> assertEquals(
                        LocalTime.of(8, 0),
                        creada.getHoraInicio()),
                () -> assertEquals(
                        LocalTime.of(15, 0),
                        creada.getHoraFin())
        );
    }

    @Test
    @DisplayName(
            "No se puede crear una agenda sin médico")
    void crearSinMedicoProduceError() {

        AgendaMedica datos = valida();
        datos.setMedicoId(0);

        assertThrows(
                DatosAgendaInvalidosException.class,
                () -> servicio.crear(
                        PERSONAL,
                        datos));
    }

    @Test
    @DisplayName(
            "No se puede crear una agenda sin fecha")
    void crearSinFechaProduceError() {

        AgendaMedica datos = valida();
        datos.setFecha(null);

        assertThrows(
                DatosAgendaInvalidosException.class,
                () -> servicio.crear(
                        PERSONAL,
                        datos));
    }

    @Test
    @DisplayName(
            "No se puede crear una agenda sin hora de inicio")
    void crearSinHoraInicioProduceError() {

        AgendaMedica datos = valida();
        datos.setHoraInicio(null);

        assertThrows(
                DatosAgendaInvalidosException.class,
                () -> servicio.crear(
                        PERSONAL,
                        datos));
    }

    @Test
    @DisplayName(
            "No se puede crear una agenda sin hora de fin")
    void crearSinHoraFinProduceError() {

        AgendaMedica datos = valida();
        datos.setHoraFin(null);

        assertThrows(
                DatosAgendaInvalidosException.class,
                () -> servicio.crear(
                        PERSONAL,
                        datos));
    }

    @Test
    @DisplayName(
            "La hora de fin debe ser posterior a la hora de inicio")
    void crearConHorarioInvalidoProduceError() {

        AgendaMedica datos = valida();

        datos.setHoraInicio(
                LocalTime.of(15, 0));
        datos.setHoraFin(
                LocalTime.of(8, 0));

        assertThrows(
                DatosAgendaInvalidosException.class,
                () -> servicio.crear(
                        PERSONAL,
                        datos));
    }

    @Test
    @DisplayName(
            "Las horas de inicio y fin no pueden ser iguales")
    void crearConHorasIgualesProduceError() {

        AgendaMedica datos = valida();

        datos.setHoraInicio(
                LocalTime.of(10, 0));
        datos.setHoraFin(
                LocalTime.of(10, 0));

        assertThrows(
                DatosAgendaInvalidosException.class,
                () -> servicio.crear(
                        PERSONAL,
                        datos));
    }

    @Test
    @DisplayName(
            "Consultar una agenda devuelve sus datos")
    void consultarAgendaExistente()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(
                        PERSONAL,
                        valida());

        AgendaMedica consultada =
                servicio.consultar(
                        PERSONAL,
                        creada.getId());

        assertAll(
                () -> assertEquals(
                        creada.getId(),
                        consultada.getId()),
                () -> assertEquals(
                        creada.getMedicoId(),
                        consultada.getMedicoId()),
                () -> assertEquals(
                        creada.getFecha(),
                        consultada.getFecha())
        );
    }

    @Test
    @DisplayName(
            "Consultar una agenda inexistente produce un error")
    void consultarAgendaInexistente() {

        assertThrows(
                AgendaNoEncontradaException.class,
                () -> servicio.consultar(
                        PERSONAL,
                        999L));
    }

    @Test
    @DisplayName(
            "Modificar una agenda actualiza sus datos")
    void modificarAgendaCorrectamente()
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
        nuevosDatos.setObservaciones(
                "Horario actualizado");

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
                        modificada.getHoraFin()),
                () -> assertEquals(
                        "Horario actualizado",
                        modificada.getObservaciones()),
                () -> assertTrue(
                        modificada.isActivo())
        );
    }

    @Test
    @DisplayName(
            "Modificar una agenda inexistente produce un error")
    void modificarAgendaInexistente() {

        assertThrows(
                AgendaNoEncontradaException.class,
                () -> servicio.modificar(
                        PERSONAL,
                        999L,
                        valida()));
    }

    @Test
    @DisplayName(
            "Modificar con datos inválidos no cambia la agenda")
    void modificarConDatosInvalidosNoCambiaAgenda()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(
                        PERSONAL,
                        valida());

        AgendaMedica nuevosDatos =
                valida();

        nuevosDatos.setFecha(null);

        assertThrows(
                DatosAgendaInvalidosException.class,
                () -> servicio.modificar(
                        PERSONAL,
                        creada.getId(),
                        nuevosDatos));

        AgendaMedica consultada =
                servicio.consultar(
                        PERSONAL,
                        creada.getId());

        assertEquals(
                LocalDate.of(2026, 11, 17),
                consultada.getFecha());
    }

    @Test
    @DisplayName(
            "Eliminar una agenda realiza una baja lógica")
    void eliminarAgendaRealizaBajaLogica()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(
                        PERSONAL,
                        valida());

        Long id = creada.getId();
        long medicoId = creada.getMedicoId();
        LocalDate fecha = creada.getFecha();

        servicio.eliminar(
                PERSONAL,
                id);

        AgendaMedica consultada =
                servicio.consultar(
                        PERSONAL,
                        id);

        assertAll(
                () -> assertFalse(
                        consultada.isActivo()),
                () -> assertEquals(
                        id,
                        consultada.getId()),
                () -> assertEquals(
                        medicoId,
                        consultada.getMedicoId()),
                () -> assertEquals(
                        fecha,
                        consultada.getFecha())
        );
    }

    @Test
    @DisplayName(
            "Eliminar una agenda inexistente produce un error")
    void eliminarAgendaInexistente() {

        assertThrows(
                AgendaNoEncontradaException.class,
                () -> servicio.eliminar(
                        PERSONAL,
                        999L));
    }

    @Test
    @DisplayName(
            "Listar por médico devuelve únicamente sus agendas")
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
            "Listar por fecha devuelve únicamente las agendas de ese día")
    void listarPorFecha()
            throws Exception {

        AgendaMedica primera = valida();

        AgendaMedica segunda = valida();
        segunda.setFecha(
                LocalDate.of(
                        2026,
                        11,
                        18));

        servicio.crear(
                PERSONAL,
                primera);
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

        assertAll(
                () -> assertEquals(
                        1,
                        resultado.size()),
                () -> assertEquals(
                        LocalDate.of(
                                2026,
                                11,
                                17),
                        resultado.get(0)
                                .getFecha())
        );
    }

    @Test
    @DisplayName(
            "Listar agendas activas devuelve solo las activas")
    void listarAgendasActivas()
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

        var resultado =
                servicio.listar(
                        PERSONAL,
                        null,
                        null,
                        true);

        assertAll(
                () -> assertEquals(
                        1,
                        resultado.size()),
                () -> assertTrue(
                        resultado.get(0)
                                .isActivo())
        );
    }

    @Test
    @DisplayName(
            "Listar agendas inactivas devuelve solo las eliminadas")
    void listarAgendasInactivas()
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

        var resultado =
                servicio.listar(
                        PERSONAL,
                        null,
                        null,
                        false);

        assertAll(
                () -> assertEquals(
                        1,
                        resultado.size()),
                () -> assertFalse(
                        resultado.get(0)
                                .isActivo())
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
            "Un administrador no puede gestionar agendas médicas")
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
            "Un médico no puede modificar una agenda")
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

        AgendaMedica nuevosDatos =
                valida();

        nuevosDatos.setHoraInicio(
                LocalTime.of(9, 0));

        assertThrows(
                SinPermisoException.class,
                () -> servicio.modificar(
                        medico,
                        creada.getId(),
                        nuevosDatos));

        AgendaMedica consultada =
                servicio.consultar(
                        PERSONAL,
                        creada.getId());

        assertEquals(
                LocalTime.of(8, 0),
                consultada.getHoraInicio());
    }

    @Test
    @DisplayName(
            "Un médico no puede eliminar una agenda")
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

        AgendaMedica consultada =
                servicio.consultar(
                        PERSONAL,
                        creada.getId());

        assertTrue(
                consultada.isActivo());
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

    @Test
    @DisplayName(
            "Un médico solo puede listar y consultar sus propias agendas")
    void medicoSoloPuedeConsultarAgendasPropias()
            throws Exception {

        AgendaMedica propia =
                servicio.crear(PERSONAL, valida());

        AgendaMedica ajenaDatos = valida();
        ajenaDatos.setMedicoId(2L);
        AgendaMedica ajena =
                servicio.crear(PERSONAL, ajenaDatos);

        Usuario medico = new Usuario(
                "medico-1", Rol.MEDICO, 1L);

        var resultado = servicio.listarPropias(
                medico);

        assertAll(
                () -> assertEquals(1, resultado.size()),
                () -> assertEquals(
                        propia.getId(),
                        resultado.get(0).getId()),
                () -> assertEquals(
                        propia.getId(),
                        servicio.consultarPropia(
                                medico, propia.getId()).getId())
        );

        assertThrows(
                SinPermisoException.class,
                () -> servicio.consultarPropia(
                        medico, ajena.getId()));
    }

    @Test
    @DisplayName(
            "Un médico crea agendas a su nombre aunque envíe otro médico")
    void crearAgendaPropiaNoPermiteSuplantarMedico()
            throws Exception {

        Usuario medico = new Usuario(
                "medico-1", Rol.MEDICO, 1L);

        AgendaMedica datos = valida();
        datos.setMedicoId(2L);

        AgendaMedica creada =
                servicio.crearPropia(medico, datos);

        assertAll(
                () -> assertEquals(1L, creada.getMedicoId()),
                () -> assertTrue(creada.isActivo()),
                () -> assertEquals(
                        1L,
                        servicio.listarPropias(
                                medico)
                                .get(0).getMedicoId())
        );
    }

    @Test
    @DisplayName(
            "Modificar conserva la propiedad de la agenda del médico")
    void modificarAgendaPropiaNoPermiteTransferirPropiedad()
            throws Exception {

        AgendaMedica creada =
                servicio.crear(PERSONAL, valida());

        Usuario medico = new Usuario(
                "medico-1", Rol.MEDICO, 1L);

        AgendaMedica cambios = valida();
        cambios.setMedicoId(2L);
        cambios.setHoraInicio(LocalTime.of(9, 0));

        AgendaMedica modificada =
                servicio.modificarPropia(
                        medico, creada.getId(), cambios);

        assertAll(
                () -> assertEquals(1L, modificada.getMedicoId()),
                () -> assertEquals(
                        LocalTime.of(9, 0),
                        modificada.getHoraInicio())
        );
    }

    @Test
    @DisplayName(
            "Modificar con datos inválidos no cambia la agenda propia")
    void modificarAgendaPropiaInvalidaNoGuardaCambios()
            throws Exception {

        Usuario medico = new Usuario(
                "medico-1", Rol.MEDICO, 1L);
        AgendaMedica creada =
                servicio.crearPropia(medico, valida());

        AgendaMedica cambios = valida();
        cambios.setHoraInicio(LocalTime.of(15, 0));
        cambios.setHoraFin(LocalTime.of(8, 0));

        assertThrows(
                DatosAgendaInvalidosException.class,
                () -> servicio.modificarPropia(
                        medico, creada.getId(), cambios));

        assertEquals(
                LocalTime.of(8, 0),
                servicio.consultarPropia(
                        medico, creada.getId())
                        .getHoraInicio());
    }

    @Test
    @DisplayName(
            "Un médico no puede modificar ni eliminar agendas ajenas")
    void medicoNoPuedeModificarNiEliminarAgendaAjena()
            throws Exception {

        AgendaMedica ajenaDatos = valida();
        ajenaDatos.setMedicoId(2L);
        AgendaMedica ajena =
                servicio.crear(PERSONAL, ajenaDatos);

        Usuario medico = new Usuario(
                "medico-1", Rol.MEDICO, 1L);

        assertAll(
                () -> assertThrows(
                        SinPermisoException.class,
                        () -> servicio.modificarPropia(
                                medico, ajena.getId(), valida())),
                () -> assertThrows(
                        SinPermisoException.class,
                        () -> servicio.eliminarPropia(
                                medico, ajena.getId()))
        );

        assertTrue(
                servicio.consultar(PERSONAL, ajena.getId())
                        .isActivo());
    }

    @Test
    @DisplayName(
            "Un médico sin identificador no puede gestionar agendas")
    void medicoSinIdNoPuedeGestionarAgendas() {

        Usuario medico = new Usuario(
                "medico", Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.listarPropias(
                        medico));
    }
}