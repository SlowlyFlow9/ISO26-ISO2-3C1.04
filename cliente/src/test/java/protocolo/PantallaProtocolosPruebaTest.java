package protocolo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

class PantallaProtocolosPruebaTest {

    @Test
    void administradorPuedeGestionar() {

        Usuario admin =
                new Usuario(
                        "admin",
                        Rol.ADMINISTRADOR);

        assertTrue(
                PantallaProtocolosPrueba
                        .puedeGestionar(admin));
    }

    @Test
    void medicoNoPuedeGestionar() {

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        assertFalse(
                PantallaProtocolosPrueba
                        .puedeGestionar(medico));
    }
}