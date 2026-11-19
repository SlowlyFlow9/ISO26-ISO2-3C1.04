package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

class PantallaCitasPacienteTest {

    private Usuario paciente;
    private CitasServiceSimulado servicio;

    @BeforeEach
    void setUp() {

        paciente =
                new Usuario(
                        "paciente1",
                        Rol.PACIENTE);

        servicio =
                new CitasServiceSimulado();

        servicio.vincularPaciente(
                "paciente1",
                10L);

        Cita primera = new Cita();

        primera.setPacienteId(10L);
        primera.setMedicoId(20L);
        primera.setTipoPruebaId(30L);

        primera.setFechaHora(
                LocalDateTime.of(
                        2026,
                        12,
                        1,
                        10,
                        0));

        primera.setEstado(
                EstadoCita.PROGRAMADA);

        servicio.agregarCita(
                primera);

        Cita segunda =
                primera.copia();

        segunda.setMedicoId(21L);

        segunda.setFechaHora(
                LocalDateTime.of(
                        2026,
                        12,
                        2,
                        12,
                        0));

        segunda.setEstado(
                EstadoCita.CONFIRMADA);

        servicio.agregarCita(
                segunda);
    }

    @Test
    @DisplayName(
            "La pantalla muestra las citas del paciente")
    void muestraCitas()
            throws Exception {

        PantallaCitasPaciente[] pantalla =
                new PantallaCitasPaciente[1];

        SwingUtilities.invokeAndWait(() ->
                pantalla[0] =
                        new PantallaCitasPaciente(
                                servicio,
                                paciente));

        JTable tabla =
                buscar(
                        pantalla[0],
                        "tablaCitas",
                        JTable.class);

        assertEquals(
                2,
                tabla.getRowCount());
    }

    @Test
    @DisplayName(
            "Seleccionar una cita muestra su detalle")
    void muestraDetalle()
            throws Exception {

        PantallaCitasPaciente[] pantalla =
                new PantallaCitasPaciente[1];

        SwingUtilities.invokeAndWait(() -> {

            pantalla[0] =
                    new PantallaCitasPaciente(
                            servicio,
                            paciente);

            JTable tabla =
                    buscar(
                            pantalla[0],
                            "tablaCitas",
                            JTable.class);

            tabla.setRowSelectionInterval(
                    0,
                    0);
        });

        JTextArea detalle =
                buscar(
                        pantalla[0],
                        "detalleCita",
                        JTextArea.class);

        assertTrue(
                detalle.getText()
                        .contains("Cita 1"));

        assertTrue(
                detalle.getText()
                        .contains("PROGRAMADA"));
    }

    @Test
    @DisplayName(
            "La pantalla está disponible para pacientes")
    void disponibleParaPaciente() {

        assertTrue(
                PantallaCitasPaciente
                        .disponiblePara(
                                paciente));
    }

    @Test
    @DisplayName(
            "La pantalla no está disponible para médicos")
    void noDisponibleParaMedico() {

        assertFalse(
                PantallaCitasPaciente
                        .disponiblePara(
                                new Usuario(
                                        "medico",
                                        Rol.MEDICO)));
    }

    @Test
    @DisplayName(
            "La pantalla no está disponible para usuario nulo")
    void noDisponibleParaNulo() {

        assertFalse(
                PantallaCitasPaciente
                        .disponiblePara(null));
    }

    private static <T extends Component> T buscar(
            Container contenedor,
            String nombre,
            Class<T> tipo) {

        for (Component componente
                : contenedor.getComponents()) {

            if (nombre.equals(
                    componente.getName())
                    && tipo.isInstance(
                            componente)) {

                return tipo.cast(
                        componente);
            }

            if (componente
                    instanceof Container hijo) {

                T encontrado =
                        buscar(
                                hijo,
                                nombre,
                                tipo);

                if (encontrado != null) {
                    return encontrado;
                }
            }
        }

        return null;
    }
}