package paciente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void soloElPersonalAdministrativoPuedeDarDeAltaPacientes() {
        for (Rol rol : Rol.values()) {
            assertEquals(rol == Rol.PERSONAL_ADMINISTRATIVO, new Usuario("x", rol).puedeDarDeAltaPacientes(), rol.name());
        }
    }

    @Test
    void laPantallaSoloEstaDisponibleParaElPersonalAdministrativo() {
        assertTrue(PantallaAltaPaciente.disponiblePara(new Usuario("x", Rol.PERSONAL_ADMINISTRATIVO)));
        assertFalse(PantallaAltaPaciente.disponiblePara(new Usuario("x", Rol.MEDICO)));
    }
}
