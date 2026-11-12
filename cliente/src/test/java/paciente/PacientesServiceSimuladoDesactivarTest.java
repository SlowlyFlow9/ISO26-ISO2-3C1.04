package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PacientesServiceSimuladoDesactivarTest {
    private static final Usuario ADMIN = new Usuario("ana", Rol.PERSONAL_ADMINISTRATIVO);

    private PacientesServiceSimulado servicio;

    @BeforeEach
    void preparar() throws Exception {
        servicio = new PacientesServiceSimulado();
        servicio.crear(ADMIN, paciente());
    }

    private Paciente paciente() {
        Paciente paciente = new Paciente();
        paciente.setNombre("Ana");
        paciente.setApellidos("Pérez");
        paciente.setDni("12345678Z");
        paciente.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        paciente.setTelefono("600111222");
        return paciente;
    }

    @Test
    void desactivarConservaLosDatosYLoExcluyeDeLaBusqueda() throws Exception {
        servicio.desactivar(ADMIN, 1L);

        Paciente guardado = servicio.consultar(ADMIN, 1L);
        assertFalse(guardado.isActivo());
        assertEquals("12345678Z", guardado.getDni());
        assertTrue(servicio.buscar(ADMIN, "Ana").isEmpty());
    }

    @Test
    void desactivarInexistenteODadoDeBajaLanzaLaExcepcionCorrespondiente() throws Exception {
        assertThrows(PacienteNoEncontradoException.class, () -> servicio.desactivar(ADMIN, 99L));
        servicio.desactivar(ADMIN, 1L);
        assertThrows(PacienteYaInactivoException.class, () -> servicio.desactivar(ADMIN, 1L));
    }

    @Test
    void soloElPersonalAdministrativoPuedeDarDeBaja() {
        for (Rol rol : Rol.values()) {
            if (rol == Rol.PERSONAL_ADMINISTRATIVO) continue;
            assertThrows(SinPermisoException.class,
                    () -> servicio.desactivar(new Usuario("x", rol), 1L), rol.name());
        }
        assertThrows(SinPermisoException.class, () -> servicio.desactivar(null, 1L));
        assertEquals(1, servicio.buscar(ADMIN, "Ana").size());
    }
}
