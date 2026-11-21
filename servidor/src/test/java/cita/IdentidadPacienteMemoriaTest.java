package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

class IdentidadPacienteMemoriaTest {

    private IdentidadPacienteMemoria identidad;

    @BeforeEach
    void setUp() {
        identidad =
                new IdentidadPacienteMemoria();
    }

    @Test
    @DisplayName(
            "Un usuario vinculado devuelve su paciente")
    void usuarioVinculado() throws Exception {

        identidad.vincular(
                "adrian",
                10L);

        long resultado =
                identidad.obtenerPacienteId(
                        new Usuario(
                                "adrian",
                                Rol.PACIENTE));

        assertEquals(
                10L,
                resultado);
    }

    @Test
    @DisplayName(
            "Un usuario no vinculado genera error")
    void usuarioNoVinculado() {

        Usuario usuario =
                new Usuario(
                        "desconocido",
                        Rol.PACIENTE);

        assertThrows(
                PacienteNoVinculadoException.class,
                () -> identidad
                        .obtenerPacienteId(
                                usuario));
    }

    @Test
    @DisplayName(
            "Una nueva vinculación sustituye la anterior")
    void actualizarVinculacion()
            throws Exception {

        identidad.vincular(
                "adrian",
                10L);

        identidad.vincular(
                "adrian",
                20L);

        assertEquals(
                20L,
                identidad.obtenerPacienteId(
                        new Usuario(
                                "adrian",
                                Rol.PACIENTE)));
    }
}