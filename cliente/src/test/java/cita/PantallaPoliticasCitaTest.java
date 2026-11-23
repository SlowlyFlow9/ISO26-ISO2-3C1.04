package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.JTextField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class PantallaPoliticasCitaTest {

    private final Usuario administrador =
            new Usuario("admin", Rol.ADMINISTRADOR);
    private ServicioFalso servicio;
    private PantallaPoliticasCita pantalla;

    @BeforeEach
    void preparar() throws Exception {
        servicio = new ServicioFalso();
        PantallaPoliticasCita[] creada =
                new PantallaPoliticasCita[1];
        SwingUtilities.invokeAndWait(() ->
                creada[0] = new PantallaPoliticasCita(
                        servicio, administrador));
        pantalla = creada[0];
    }

    private static class ServicioFalso
            implements PoliticaCitaService {
        private PoliticaCita politica = PoliticaCita.inicial();
        private int modificaciones;

        @Override
        public PoliticaCita consultar(Usuario solicitante)
                throws SinPermisoException {
            if (solicitante.rol() != Rol.ADMINISTRADOR) {
                throw new SinPermisoException("Sin permiso");
            }
            return politica;
        }

        @Override
        public PoliticaCita modificar(
                Usuario solicitante,
                PoliticaCita nueva)
                throws SinPermisoException {
            if (solicitante.rol() != Rol.ADMINISTRADOR) {
                throw new SinPermisoException("Sin permiso");
            }
            modificaciones++;
            politica = nueva;
            return politica;
        }
    }

    private static <T extends Component> T buscar(
            Container contenedor,
            String nombre,
            Class<T> tipo) {
        for (Component componente : contenedor.getComponents()) {
            if (nombre.equals(componente.getName())
                    && tipo.isInstance(componente)) {
                return tipo.cast(componente);
            }
            if (componente instanceof Container hijo) {
                T encontrado = buscar(hijo, nombre, tipo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    private JTextField campo(String nombre) {
        return buscar(pantalla, nombre, JTextField.class);
    }

    @Test
    void cargarValoresYGuardarPolitica() throws Exception {
        assertEquals("90", campo("antelacionMaximaDias").getText());

        SwingUtilities.invokeAndWait(() -> {
            campo("antelacionMinimaHoras").setText("12");
            campo("antelacionMaximaDias").setText("120");
            campo("cancelacionMinimaHoras").setText("48");
            campo("maxCitasActivasPorPaciente").setText("4");
            buscar(pantalla, "guardar", JButton.class).doClick();
        });

        assertEquals(1, servicio.modificaciones);
        assertEquals(12,
                servicio.politica.getAntelacionMinimaHoras());
        assertEquals(4,
                servicio.politica.getMaxCitasActivasPorPaciente());
        assertEquals("Politica guardada",
                buscar(pantalla, "mensaje", JLabel.class).getText());
    }

    @Test
    void formatoInvalidoNoSeEnviaAlServicio() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            campo("antelacionMinimaHoras").setText("doce");
            buscar(pantalla, "guardar", JButton.class).doClick();
        });

        assertEquals(0, servicio.modificaciones);
        assertTrue(buscar(pantalla, "mensaje", JLabel.class)
                .getText().contains("entero"));
    }

    @Test
    void usuarioSinRolAdministradorNoVeElFormulario()
            throws Exception {
        PantallaPoliticasCita[] creada =
                new PantallaPoliticasCita[1];
        SwingUtilities.invokeAndWait(() ->
                creada[0] = new PantallaPoliticasCita(
                        servicio,
                        new Usuario("medico", Rol.MEDICO)));

        assertNull(buscar(creada[0], "guardar", JButton.class));
        assertNull(buscar(
                creada[0],
                "antelacionMinimaHoras",
                JTextField.class));
        assertFalse(PantallaPoliticasCita.disponiblePara(
                new Usuario("medico", Rol.MEDICO)));
        assertTrue(buscar(creada[0], "mensaje", JLabel.class)
                .getText().contains("Administrador"));
    }
}
