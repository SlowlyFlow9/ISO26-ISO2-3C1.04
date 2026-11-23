package cita;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU02 - Pruebas del boton Confirmar cita en Swing.
 * Los dialogos se simulan y no se abren ventanas.
 */

class PantallaCitasConfirmacionTest {

    private Usuario paciente;
    private CitasServiceSimulado servicio;

    @BeforeEach
    void preparar() {

        paciente = new Usuario(
                "paciente1", Rol.PACIENTE);

        servicio = new CitasServiceSimulado();

        servicio.vincularPaciente(
                "paciente1", 10L);

        servicio.agregarCita(
                crearCita(EstadoCita.PROGRAMADA));

        servicio.agregarCita(
                crearCita(EstadoCita.CONFIRMADA));
    }

    private Cita crearCita(EstadoCita estado) {

        Cita cita = new Cita();

        cita.setPacienteId(10L);
        cita.setMedicoId(20L);
        cita.setTipoPruebaId(30L);

        cita.setFechaHora(
                LocalDateTime.of(
                        2026, 12, 10, 10, 0));

        cita.setEstado(estado);

        return cita;
    }

    private PantallaCitasPaciente crearPantalla(
            BiPredicate<Component, Long> dialogo)
            throws Exception {

        PantallaCitasPaciente[] resultado =
                new PantallaCitasPaciente[1];

        SwingUtilities.invokeAndWait(() -> {
            resultado[0] =
                    new PantallaCitasPaciente(
                            servicio, paciente, dialogo);
        });

        return resultado[0];
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

                T encontrado =
                        buscar(hijo, nombre, tipo);

                if (encontrado != null) {
                    return encontrado;
                }
            }
        }

        return null;
    }

    @Test
    void botonDesactivadoSinSeleccion()
            throws Exception {

        var pantalla = crearPantalla(
                (padre, id) -> true);

        JButton boton = buscar(
                pantalla,
                "botonConfirmarCita",
                JButton.class);

        assertNotNull(boton);
        assertFalse(boton.isEnabled());
    }

    @Test
    void citaProgramadaPermiteConfirmar()
            throws Exception {

        var pantalla = crearPantalla(
                (padre, id) -> true);

        JTable tabla = buscar(
                pantalla, "tablaCitas", JTable.class);

        JButton boton = buscar(
                pantalla,
                "botonConfirmarCita",
                JButton.class);

        SwingUtilities.invokeAndWait(() ->
                tabla.setRowSelectionInterval(0, 0));

        assertTrue(boton.isEnabled());
    }

    @Test
    void citaConfirmadaNoPermiteConfirmar()
            throws Exception {

        var pantalla = crearPantalla(
                (padre, id) -> true);

        JTable tabla = buscar(
                pantalla, "tablaCitas", JTable.class);

        JButton boton = buscar(
                pantalla,
                "botonConfirmarCita",
                JButton.class);

        SwingUtilities.invokeAndWait(() ->
                tabla.setRowSelectionInterval(1, 1));

        assertFalse(boton.isEnabled());
    }

    @Test
    void aceptarConfirmacionActualizaPantalla()
            throws Exception {

        AtomicInteger dialogos =
                new AtomicInteger();

        var pantalla = crearPantalla((padre, id) -> {

            dialogos.incrementAndGet();
            return true;
        });

        JTable tabla = buscar(
                pantalla, "tablaCitas", JTable.class);

        JButton boton = buscar(
                pantalla,
                "botonConfirmarCita",
                JButton.class);

        JTextArea detalle = buscar(
                pantalla,
                "detalleCita",
                JTextArea.class);

        JLabel mensaje = buscar(
                pantalla,
                "mensajeCitas",
                JLabel.class);

        SwingUtilities.invokeAndWait(() -> {

            tabla.setRowSelectionInterval(0, 0);
            boton.doClick();
        });

        assertEquals(1, dialogos.get());

        assertEquals(
                EstadoCita.CONFIRMADA,
                servicio.consultarMiCita(
                        paciente, 1L).getEstado());

        assertEquals(
                EstadoCita.CONFIRMADA,
                tabla.getValueAt(0, 4));

        assertTrue(
                detalle.getText().contains("CONFIRMADA"));

        assertTrue(
                mensaje.getText().contains(
                        "confirmada correctamente"));

        assertFalse(boton.isEnabled());
    }

    @Test
    void rechazarDialogoNoModificaCita()
            throws Exception {

        AtomicInteger dialogos =
                new AtomicInteger();

        var pantalla = crearPantalla((padre, id) -> {

            dialogos.incrementAndGet();
            return false;
        });

        JTable tabla = buscar(
                pantalla, "tablaCitas", JTable.class);

        JButton boton = buscar(
                pantalla,
                "botonConfirmarCita",
                JButton.class);

        SwingUtilities.invokeAndWait(() -> {

            tabla.setRowSelectionInterval(0, 0);
            boton.doClick();
        });

        assertEquals(1, dialogos.get());

        assertEquals(
                EstadoCita.PROGRAMADA,
                servicio.consultarMiCita(
                        paciente, 1L).getEstado());

        assertEquals(
                EstadoCita.PROGRAMADA,
                tabla.getValueAt(0, 4));
    }

    @Test
    void cambiarSeleccionActualizaBoton()
            throws Exception {

        var pantalla = crearPantalla(
                (padre, id) -> true);

        JTable tabla = buscar(
                pantalla, "tablaCitas", JTable.class);

        JButton boton = buscar(
                pantalla,
                "botonConfirmarCita",
                JButton.class);

        SwingUtilities.invokeAndWait(() ->
                tabla.setRowSelectionInterval(0, 0));

        assertTrue(boton.isEnabled());

        SwingUtilities.invokeAndWait(() ->
                tabla.setRowSelectionInterval(1, 1));

        assertFalse(boton.isEnabled());

        SwingUtilities.invokeAndWait(
                tabla::clearSelection);

        assertFalse(boton.isEnabled());
    }
}