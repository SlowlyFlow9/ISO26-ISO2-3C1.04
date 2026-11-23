package agenda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class PantallaHorarioServicioTest {

    private final Usuario administrador =
            new Usuario("admin", Rol.ADMINISTRADOR);
    private ServicioFalso servicio;
    private PantallaHorarioServicio pantalla;

    @BeforeEach
    void preparar() throws Exception {
        servicio = new ServicioFalso();
        PantallaHorarioServicio[] creada =
                new PantallaHorarioServicio[1];
        SwingUtilities.invokeAndWait(() ->
                creada[0] = new PantallaHorarioServicio(
                        servicio, administrador));
        pantalla = creada[0];
    }

    private static class ServicioFalso
            implements HorarioServicioService {
        private HorarioServicio horario = HorarioServicio.vacio();
        private int modificaciones;

        @Override
        public HorarioServicio consultar(Usuario solicitante)
                throws SinPermisoException {
            if (solicitante.rol() != Rol.ADMINISTRADOR) {
                throw new SinPermisoException("Sin permiso");
            }
            return horario;
        }

        @Override
        public HorarioServicio modificar(
                Usuario solicitante,
                HorarioServicio nuevo)
                throws SinPermisoException {
            if (solicitante.rol() != Rol.ADMINISTRADOR) {
                throw new SinPermisoException("Sin permiso");
            }
            modificaciones++;
            horario = nuevo;
            return horario;
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

    private JTextField campo(DayOfWeek dia) {
        return buscar(pantalla,
                "intervalos" + dia.name(), JTextField.class);
    }

    @Test
    void cargaHorarioYPermiteGuardarCambios() throws Exception {
        servicio.horario.getDias().get(0).getIntervalos().add(
                new IntervaloHorario(
                        LocalTime.of(8, 0),
                        LocalTime.of(12, 0)));
        SwingUtilities.invokeAndWait(() ->
                pantalla = new PantallaHorarioServicio(
                        servicio, administrador));

        assertEquals("08:00-12:00", campo(DayOfWeek.MONDAY).getText());

        SwingUtilities.invokeAndWait(() -> {
            campo(DayOfWeek.MONDAY)
                    .setText("08:00-12:00, 13:00-17:00");
            buscar(pantalla, "guardar", JButton.class).doClick();
        });

        assertEquals(1, servicio.modificaciones);
        assertEquals(2, servicio.horario.getDias().get(0)
                .getIntervalos().size());
        assertEquals("Horario guardado",
                buscar(pantalla, "mensaje", JLabel.class).getText());
    }

    @Test
    void formatoInvalidoNoSeEnviaAlServicio() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            campo(DayOfWeek.MONDAY).setText("8:00-12:00");
            buscar(pantalla, "guardar", JButton.class).doClick();
        });

        assertEquals(0, servicio.modificaciones);
        assertTrue(buscar(pantalla, "mensaje", JLabel.class)
                .getText().contains("formato"));
    }

    @Test
    void usuarioSinRolAdministradorNoPuedeConfigurar()
            throws Exception {
        PantallaHorarioServicio[] creada =
                new PantallaHorarioServicio[1];
        SwingUtilities.invokeAndWait(() ->
                creada[0] = new PantallaHorarioServicio(
                        servicio,
                        new Usuario("medico", Rol.MEDICO)));

        JButton boton = buscar(
                creada[0], "guardar", JButton.class);
        assertFalse(boton.isVisible());
        assertFalse(PantallaHorarioServicio.disponiblePara(
                new Usuario("medico", Rol.MEDICO)));
        assertTrue(buscar(
                creada[0], "mensaje", JLabel.class)
                .getText().contains("Administrador"));
    }
}
