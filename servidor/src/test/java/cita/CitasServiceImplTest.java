package cita;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class CitasServiceImplTest {

    private static final Usuario PACIENTE_1 =
            new Usuario(
                    "paciente1",
                    Rol.PACIENTE);

    private static final Usuario PACIENTE_2 =
            new Usuario(
                    "paciente2",
                    Rol.PACIENTE);

    private CitasRepositorioMemoria repositorio;
    private IdentidadPacienteMemoria identidad;
    private CitasServiceImpl servicio;

    private Cita citaPaciente1;
    private Cita citaPaciente2;

    @BeforeEach
    void setUp() {

        repositorio =
                new CitasRepositorioMemoria();

        identidad =
                new IdentidadPacienteMemoria();

        identidad.vincular(
                "paciente1",
                10L);

        identidad.vincular(
                "paciente2",
                20L);

        servicio =
                ServiciosCitasPrueba.citas(
                        repositorio,
                        identidad);

        citaPaciente1 =
                crearCita(
                        10L,
                        30L,
                        40L,
                        LocalDateTime.of(
                                2026,
                                12,
                                1,
                                9,
                                30));

        repositorio.guardar(
                citaPaciente1);

        repositorio.guardar(
                crearCita(
                        10L,
                        31L,
                        41L,
                        LocalDateTime.of(
                                2026,
                                12,
                                3,
                                11,
                                0)));

        citaPaciente2 =
                crearCita(
                        20L,
                        32L,
                        42L,
                        LocalDateTime.of(
                                2026,
                                12,
                                5,
                                12,
                                30));

        repositorio.guardar(
                citaPaciente2);
    }

    private Cita crearCita(
            long pacienteId,
            long medicoId,
            long tipoPruebaId,
            LocalDateTime fechaHora) {

        Cita cita = new Cita();

        cita.setPacienteId(
                pacienteId);

        cita.setMedicoId(
                medicoId);

        cita.setTipoPruebaId(
                tipoPruebaId);

        cita.setFechaHora(
                fechaHora);

        cita.setEstado(
                EstadoCita.PROGRAMADA);

        return cita;
    }

    @Test
    @DisplayName(
            "Un paciente obtiene únicamente sus citas")
    void listarMisCitas() throws Exception {

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

        identidad.vincular(
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
            "Un paciente puede consultar el detalle de su cita")
    void consultarMiCita()
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
                        30L,
                        resultado.getMedicoId()),

                () -> assertEquals(
                        40L,
                        resultado.getTipoPruebaId()),

                () -> assertEquals(
                        EstadoCita.PROGRAMADA,
                        resultado.getEstado()),

                () -> assertEquals(
                        LocalDateTime.of(
                                2026,
                                12,
                                1,
                                9,
                                30),
                        resultado.getFechaHora())
        );
    }

    @Test
    @DisplayName(
            "Consultar una cita inexistente genera error")
    void citaInexistente() {

        assertThrows(
                CitaNoEncontradaException.class,
                () -> servicio
                        .consultarMiCita(
                                PACIENTE_1,
                                999L));
    }

    @Test
    @DisplayName(
            "Un paciente no puede consultar una cita de otro paciente")
    void noPuedeConsultarCitaAjena() {

        assertThrows(
                CitaNoEncontradaException.class,
                () -> servicio
                        .consultarMiCita(
                                PACIENTE_1,
                                citaPaciente2.getId()));
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
                        .listarMisCitas(
                                medico));
    }

    @Test
    @DisplayName(
            "Personal administrativo no puede consultar citas como paciente")
    void personalAdministrativoSinPermiso() {

        Usuario personal =
                new Usuario(
                        "personal",
                        Rol.PERSONAL_ADMINISTRATIVO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio
                        .listarMisCitas(
                                personal));
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
    void usuarioNuloSinPermiso() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio
                        .listarMisCitas(
                                null));
    }

    @Test
    @DisplayName(
            "Un paciente sin vinculación no puede consultar citas")
    void pacienteSinVinculacion() {

        Usuario desconocido =
                new Usuario(
                        "sinVincular",
                        Rol.PACIENTE);

        assertThrows(
                PacienteNoVinculadoException.class,
                () -> servicio
                        .listarMisCitas(
                                desconocido));
    }

    @Test
    @DisplayName(
            "Un médico tampoco puede consultar el detalle de una cita")
    void medicoNoPuedeConsultarDetalle() {

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio
                        .consultarMiCita(
                                medico,
                                citaPaciente1.getId()));
    }
}