package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PacienteModeloYExcepcionesTest {

    @Test
    @DisplayName(
            "Paciente conserva todos sus campos")
    void pacienteConservaDatos() {

        Paciente p = new Paciente();

        LocalDate nacimiento =
                LocalDate.of(1990, 5, 1);

        LocalDate alta =
                LocalDate.of(2026, 11, 16);

        p.setId(10L);
        p.setNombre("Ana");
        p.setApellidos("Pérez");
        p.setDni("12345678Z");
        p.setFechaNacimiento(nacimiento);
        p.setTelefono("600111222");
        p.setEmail("ana@test.es");
        p.setDireccion("Calle Mayor");
        p.setNumeroTarjetaSanitaria("T123");
        p.setFechaAlta(alta);
        p.setActivo(false);

        assertEquals(10L, p.getId());
        assertEquals("Ana", p.getNombre());
        assertEquals("Pérez", p.getApellidos());
        assertEquals("12345678Z", p.getDni());
        assertEquals(nacimiento, p.getFechaNacimiento());
        assertEquals("600111222", p.getTelefono());
        assertEquals("ana@test.es", p.getEmail());
        assertEquals("Calle Mayor", p.getDireccion());
        assertEquals(
                "T123",
                p.getNumeroTarjetaSanitaria());
        assertEquals(alta, p.getFechaAlta());
        assertFalse(p.isActivo());
    }

    @Test
    @DisplayName(
            "DatosInvalidosException conserva los errores")
    void datosInvalidosConservaErrores() {

        DatosInvalidosException ex =
                new DatosInvalidosException(
                        List.of(
                                "Error uno",
                                "Error dos"));

        assertEquals(
                "Error uno; Error dos",
                ex.getMessage());

        assertEquals(
                2,
                ex.getErrores().size());
    }

    @Test
    @DisplayName(
            "Excepciones de paciente conservan su mensaje")
    void excepcionesConservanMensaje() {

        assertEquals(
                "duplicado",
                new PacienteDuplicadoException(
                        "duplicado")
                        .getMessage());

        assertEquals(
                "no encontrado",
                new PacienteNoEncontradoException(
                        "no encontrado")
                        .getMessage());

        assertEquals(
                "inactivo",
                new PacienteYaInactivoException(
                        "inactivo")
                        .getMessage());

        assertEquals(
                "sin permiso",
                new SinPermisoException(
                        "sin permiso")
                        .getMessage());
    }

    @Test
    @DisplayName(
            "ServicioNoDisponibleException conserva causa y mensaje")
    void servicioNoDisponibleConCausa() {

        RuntimeException causa =
                new RuntimeException("causa");

        ServicioNoDisponibleException ex =
                new ServicioNoDisponibleException(
                        "servidor caído",
                        causa);

        assertEquals(
                "servidor caído",
                ex.getMessage());

        assertSame(
                causa,
                ex.getCause());
    }

    @Test
    @DisplayName(
            "ServicioNoDisponibleException admite solo mensaje")
    void servicioNoDisponibleSoloMensaje() {

        ServicioNoDisponibleException ex =
                new ServicioNoDisponibleException(
                        "sin conexión");

        assertEquals(
                "sin conexión",
                ex.getMessage());
    }
}