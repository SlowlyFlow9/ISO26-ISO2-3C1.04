package cita;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class CitasServiceSimuladoTest {

    private static final Usuario PACIENTE_1 =
            new Usuario(
                    "paciente1",
                    Rol.PACIENTE);

    private static final Usuario PACIENTE_2 =
            new Usuario(
                    "paciente2",
                    Rol.PACIENTE);

    private CitasServiceSimulado servicio;

    private Cita citaPaciente1;
    private Cita citaPaciente2;

    @BeforeEach
    void setUp() {

        servicio =
                new CitasServiceSimulado();

        servicio.vincularPaciente(
                "paciente1",
                10L);

        servicio.vincularPaciente(
                "paciente2",
                20L);

        citaPaciente1 =
                servicio.agregarCita(
                        cita(
                                10L,
                                30L,
                                40L,
                                LocalDateTime.of(
                                        2026,
                                        12,
                                        1,
                                        10,
                                        0)));

        servicio.agregarCita(
                cita(
                        10L,
                        31L,
                        41L,
                        LocalDateTime.of(
                                2026,
                                12,
                                2,
                                11,
                                30)));

        citaPaciente2 =
                servicio.agregarCita(
                        cita(
                                20L,
                                32L,
                                42L,
                                LocalDateTime.of(
                                        2026,
                                        12,
                                        3,
                                        12,
                                        0)));
    }

    private Cita cita(
            long pacienteId,
            long medicoId,
            long tipoPruebaId,
            LocalDateTime fecha) {

        Cita cita = new Cita();

        cita.setPacienteId(
                pacienteId);

        cita.setMedicoId(
                medicoId);

        cita.setTipoPruebaId(
                tipoPruebaId);

        cita.setFechaHora(
                fecha);

        cita.setEstado(
                EstadoCita.PROGRAMADA);

        return cita;
    }

    @Test
    @DisplayName(
            "Agregar una cita asigna identificador")
    void agregarAsignaId() {

        Cita creada =
                servicio.agregarCita(
                        cita(
                                10L,
                                50L,
                                60L,
                                LocalDateTime.now()));

        assertNotNull(
                creada.getId());
    }

    @Test
    @DisplayName(
            "No se puede agregar una cita nula")
    void agregarNula() {

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio
                        .agregarCita(null));
    }

    @Test
    @DisplayName(
            "Una vinculación necesita usuario y paciente válidos")
    void vinculacionInvalida() {

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> servicio
                                .vincularPaciente(
                                        null,
                                        1L)),

                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> servicio
                                .vincularPaciente(
                                        "paciente",
                                        0L))
        );
    }

    @Test
    @DisplayName(
            "El paciente obtiene únicamente sus citas")
    void listarMisCitas()
            throws Exception {

        var resultado =
                servicio.listarMisCitas(
                        PACIENTE_1);

        assertEquals(
                2,
                resultado.size());

        assertTrue(
                resultado.stream()
                        .allMatch(c ->
                                c.getPacienteId()
                                        == 10L));
    }

    @Test
    @DisplayName(
            "Un paciente sin citas obtiene una lista vacía")
    void pacienteSinCitas()
            throws Exception {

        servicio.vincularPaciente(
                "paciente3",
                30L);

        Usuario paciente3 =
                new Usuario(
                        "paciente3",
                        Rol.PACIENTE);

        assertTrue(
                servicio.listarMisCitas(
                        paciente3)
                        .isEmpty());
    }

    @Test
    @DisplayName(
            "El paciente puede consultar una cita propia")
    void consultarCitaPropia()
            throws Exception {

        Cita resultado =
                servicio.consultarMiCita(
                        PACIENTE_1,
                        citaPaciente1.getId());

        assertAll(
                () -> assertEquals(
                        citaPaciente1.getId(),
                        resultado.getId()),

                () -> assertEquals(
                        10L,
                        resultado.getPacienteId()),

                () -> assertEquals(
                        EstadoCita.PROGRAMADA,
                        resultado.getEstado())
        );
    }

    @Test
    @DisplayName(
            "Consultar una cita inexistente genera error")
    void consultarInexistente() {

        assertThrows(
                CitaNoEncontradaException.class,
                () -> servicio
                        .consultarMiCita(
                                PACIENTE_1,
                                999L));
    }

    @Test
    @DisplayName(
            "Un paciente no puede consultar una cita ajena")
    void consultarCitaAjena() {

        assertThrows(
                CitaNoEncontradaException.class,
                () -> servicio
                        .consultarMiCita(
                                PACIENTE_1,
                                citaPaciente2.getId()));
    }

    @Test
    @DisplayName(
            "Un usuario sin vinculación genera error")
    void pacienteNoVinculado() {

        Usuario usuario =
                new Usuario(
                        "desconocido",
                        Rol.PACIENTE);

        assertThrows(
                PacienteNoVinculadoException.class,
                () -> servicio
                        .listarMisCitas(usuario));
    }

    @Test
    @DisplayName(
            "Un médico no puede consultar citas como paciente")
    void medicoSinPermiso() {

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio
                        .listarMisCitas(medico));
    }

    @Test
    @DisplayName(
            "Personal administrativo no puede consultar citas como paciente")
    void personalSinPermiso() {

        Usuario personal =
                new Usuario(
                        "personal",
                        Rol.PERSONAL_ADMINISTRATIVO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio
                        .listarMisCitas(personal));
    }

    @Test
    @DisplayName(
            "Un administrador no puede consultar citas como paciente")
    void administradorSinPermiso() {

        Usuario administrador =
                new Usuario(
                        "admin",
                        Rol.ADMINISTRADOR);

        assertThrows(
                SinPermisoException.class,
                () -> servicio
                        .listarMisCitas(
                                administrador));
    }

    @Test
    @DisplayName(
            "Un usuario nulo no puede consultar citas")
    void usuarioNulo() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio
                        .listarMisCitas(null));
    }

    @Test
    @DisplayName(
            "Las citas devueltas son copias independientes")
    void devuelveCopias()
            throws Exception {

        Cita primera =
                servicio.listarMisCitas(
                        PACIENTE_1)
                        .get(0);

        Cita segunda =
                servicio.listarMisCitas(
                        PACIENTE_1)
                        .get(0);

        assertNotSame(
                primera,
                segunda);

        primera.setEstado(
                EstadoCita.CANCELADA);

        assertEquals(
                EstadoCita.PROGRAMADA,
                segunda.getEstado());
    }
}