package solicitud;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

/**
 * Pruebas del control de acceso a la pantalla de prescripcion.
 * CU11 - Cliente.
 */
public class PantallaPrescribirPruebaTest {

    @Test
    public void medicoPuedeAcceder() {

        Usuario medico = new Usuario("Ana", Rol.MEDICO);

        assertTrue(
                PantallaPrescribirPrueba.disponiblePara(medico));
    }

    @Test
    public void pacienteNoPuedeAcceder() {

        Usuario paciente = new Usuario("Luis", Rol.PACIENTE);

        assertFalse(
                PantallaPrescribirPrueba.disponiblePara(paciente));
    }

    @Test
    public void administrativoNoPuedeAcceder() {

        Usuario administrativo = new Usuario(
                "Carlos", Rol.PERSONAL_ADMINISTRATIVO);

        assertFalse(
                PantallaPrescribirPrueba.disponiblePara(
                        administrativo));
    }

    @Test
    public void usuarioNuloNoPuedeAcceder() {

        assertFalse(
                PantallaPrescribirPrueba.disponiblePara(null));
    }
}