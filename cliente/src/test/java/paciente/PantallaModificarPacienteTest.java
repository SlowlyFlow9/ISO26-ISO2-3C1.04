package paciente;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PantallaModificarPacienteTest {

    private boolean disponiblePara(Rol rol) {
        // Preparar
        Usuario usuario = new Usuario("usuario", rol);
        // Ejecutar
        return PantallaModificarPaciente.disponiblePara(usuario);
    }

    @Test
    @DisplayName("La pantalla de baja está disponible para el Personal administrativo")
    void disponibleParaPersonalAdministrativo() {
        // Comprobar
        assertTrue(disponiblePara(Rol.PERSONAL_ADMINISTRATIVO));
    }

    @Test
    @DisplayName("La pantalla de baja no está disponible para el rol PACIENTE")
    void noDisponibleParaPaciente() { assertFalse(disponiblePara(Rol.PACIENTE)); }

    @Test
    @DisplayName("La pantalla de baja no está disponible para el rol MEDICO")
    void noDisponibleParaMedico() { assertFalse(disponiblePara(Rol.MEDICO)); }

    @Test
    @DisplayName("La pantalla de baja no está disponible para el rol ADMINISTRADOR")
    void noDisponibleParaAdministrador() { assertFalse(disponiblePara(Rol.ADMINISTRADOR)); }

    @Test
    @DisplayName("La pantalla de baja no está disponible sin usuario")
    void noDisponibleSinUsuario() {
        // Ejecutar y comprobar
        assertFalse(PantallaModificarPaciente.disponiblePara(null));
    }
}
