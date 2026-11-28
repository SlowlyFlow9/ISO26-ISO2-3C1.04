package cita;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU06: Pruebas de la pantalla Swing.
 */
class PantallaHistorialPruebasTest {

    private CitasServiceSimulado citas;
    private HistorialPruebasService servicio;
    private Usuario paciente;

    @BeforeEach
    void preparar() {

        paciente = new Usuario(
                "paciente1", Rol.PACIENTE);

        citas = new CitasServiceSimulado();
        citas.vincularPaciente("paciente1", 10L);

        servicio = new HistorialPruebasServiceSimulado(citas);
    }

    private void crearCita(
            long pacienteId, EstadoCita estado) {

        Cita cita = new Cita();
        cita.setPacienteId(pacienteId);
        cita.setMedicoId(20L);
        cita.setTipoPruebaId(30L);
        cita.setFechaHora(
                LocalDateTime.of(2026, 11, 20, 10, 0));
        cita.setEstado(estado);

        citas.agregarCita(cita);
    }

    private PantallaHistorialPruebas crearPantalla()
            throws Exception {

        PantallaHistorialPruebas[] pantalla =
                new PantallaHistorialPruebas[1];

        SwingUtilities.invokeAndWait(() ->
                pantalla[0] =
                        new PantallaHistorialPruebas(
                                servicio, paciente));

        return pantalla[0];
    }

    @Test
    void muestraPruebasRealizadas() throws Exception {

        crearCita(10L, EstadoCita.REALIZADA);
        crearCita(10L, EstadoCita.PROGRAMADA);

        var pantalla = crearPantalla();

        JTable tabla = buscar(
                pantalla, "tablaHistorial", JTable.class);

        assertNotNull(tabla);
        assertEquals(1, tabla.getRowCount());
        assertEquals(
                EstadoCita.REALIZADA,
                tabla.getValueAt(0, 4));
    }

    @Test
    void noMuestraPruebasAjenas() throws Exception {

        crearCita(10L, EstadoCita.REALIZADA);
        crearCita(20L, EstadoCita.REALIZADA);

        JTable tabla = buscar(
                crearPantalla(),
                "tablaHistorial", JTable.class);

        assertEquals(1, tabla.getRowCount());
    }

    @Test
    void mostrarHistorialVacio() throws Exception {

        var pantalla = crearPantalla();

        JTable tabla = buscar(
                pantalla, "tablaHistorial", JTable.class);

        JLabel mensaje = buscar(
                pantalla, "mensajeHistorial", JLabel.class);

        assertEquals(0, tabla.getRowCount());
        assertTrue(mensaje.getText().contains(
                "No tienes pruebas realizadas"));
    }

    @Test
    void mostrarDetalle() throws Exception {

        crearCita(10L, EstadoCita.REALIZADA);

        var pantalla = crearPantalla();

        SwingUtilities.invokeAndWait(() -> {
            JTable tabla = buscar(
                    pantalla, "tablaHistorial", JTable.class);

            tabla.setRowSelectionInterval(0, 0);
        });

        JTextArea detalle = buscar(
                pantalla, "detalleHistorial", JTextArea.class);

        assertTrue(detalle.getText().contains("Prueba 1"));
        assertTrue(detalle.getText().contains("REALIZADA"));
        assertTrue(detalle.getText().contains("20/11/2026"));
    }

    @Test
    void actualizarHistorial() throws Exception {

        var pantalla = crearPantalla();

        crearCita(10L, EstadoCita.REALIZADA);

        SwingUtilities.invokeAndWait(
                pantalla::actualizar);

        JTable tabla = buscar(
                pantalla, "tablaHistorial", JTable.class);

        assertEquals(1, tabla.getRowCount());
    }

    @Test
    void mostrarErrorSinPermiso() throws Exception {

        paciente = new Usuario(
                "medico1", Rol.MEDICO);

        var pantalla = crearPantalla();

        JLabel mensaje = buscar(
                pantalla, "mensajeHistorial", JLabel.class);

        assertTrue(mensaje.getText().startsWith("Error:"));
    }

    @Test
    void comprobarPermisosPantalla() {

        assertTrue(
                PantallaHistorialPruebas.disponiblePara(
                        paciente));

        assertFalse(
                PantallaHistorialPruebas.disponiblePara(
                        new Usuario("medico", Rol.MEDICO)));

        assertFalse(
                PantallaHistorialPruebas.disponiblePara(null));
    }

    @Test
    void servicioSimuladoFiltraCorrectamente()
            throws Exception {

        crearCita(10L, EstadoCita.PROGRAMADA);
        crearCita(10L, EstadoCita.REALIZADA);
        crearCita(20L, EstadoCita.REALIZADA);

        var resultado =
                servicio.listarMiHistorial(paciente);

        assertEquals(1, resultado.size());
        assertEquals(10L, resultado.get(0).getPacienteId());
    }

    private static <T extends Component> T buscar(
            Container contenedor,
            String nombre,
            Class<T> tipo) {

        for (Component componente :
                contenedor.getComponents()) {

            if (nombre.equals(componente.getName())
                    && tipo.isInstance(componente)) {

                return tipo.cast(componente);
            }

            if (componente instanceof Container hijo) {

                T encontrado = buscar(
                        hijo, nombre, tipo);

                if (encontrado != null) {
                    return encontrado;
                }
            }
        }

        return null;
    }
}
