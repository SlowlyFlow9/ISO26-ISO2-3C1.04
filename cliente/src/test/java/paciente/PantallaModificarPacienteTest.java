package paciente;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PantallaModificarPacienteTest {
    @Test
    void pantallaDeBajaSoloEstaDisponibleParaPersonalAdministrativo() {
        assertTrue(PantallaModificarPaciente.disponiblePara(
                new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO)));
        for (Rol rol : Rol.values()) {
            if (rol != Rol.PERSONAL_ADMINISTRATIVO) {
                assertFalse(PantallaModificarPaciente.disponiblePara(new Usuario("usuario", rol)));
            }
        }
        assertFalse(PantallaModificarPaciente.disponiblePara(null));
    }
}
