package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
 * CU04 - Pruebas del botón Cancelar cita en Swing.
 * Los diálogos se simulan y no se abren ventanas.
 */
class PantallaCitasCancelacionTest {

    private static final LocalDateTime CITA =
            LocalDateTime.of(2026, 12, 10, 10, 0);
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private Usuario paciente;
    private CitasServiceSimulado servicio;

    @BeforeEach
    void preparar() {

        paciente = new Usuario("paciente1", Rol.PACIENTE);

        servicio = new CitasServiceSimulado();
        servicio.vincularPaciente("paciente1", 10L);

        //filas 0 a 3: ids 1 a 4
        servicio.agregarCita(crearCita(EstadoCita.PROGRAMADA));
        servicio.agregarCita(crearCita(EstadoCita.CONFIRMADA));
        servicio.agregarCita(crearCita(EstadoCita.CANCELADA));
        servicio.agregarCita(crearCita(EstadoCita.REALIZADA));

        servicio.fijarReloj(Clock.fixed(
                CITA.minusHours(48).atZone(ZONA).toInstant(), ZONA));
    }

    private Cita crearCita(EstadoCita estado) {

        Cita cita = new Cita();
        cita.setPacienteId(10L);
        cita.setMedicoId(20L);
        cita.setTipoPruebaId(30L);
        cita.setFechaHora(CITA);
        cita.setEstado(estado);

        return cita;
    }

    private PantallaCitasPaciente crearPantalla(
            BiPredicate<Component, Long> confirmar,
            BiPredicate<Component, Long> cancelar)
            throws Exception {

        PantallaCitasPaciente[] resultado =
                new PantallaCitasPaciente[1];

        SwingUtilities.invokeAndWait(() ->
                resultado[0] = new PantallaCitasPaciente(
                        servicio, paciente, confirmar, cancelar));

        return resultado[0];
    }

    private PantallaCitasPaciente crearPantalla(
            BiPredicate<Component, Long> cancelar)
            throws Exception {

        return crearPantalla((padre, id) -> true, cancelar);
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

    private void seleccionar(JTable tabla, int fila)
            throws Exception {

        SwingUtilities.invokeAndWait(() ->
                tabla.setRowSelectionInterval(fila, fila));
    }

    @Test
    void botonDesactivadoSinSeleccion() throws Exception {

        var pantalla = crearPantalla((padre, id) -> true);

        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);

        assertNotNull(boton);
        assertFalse(boton.isEnabled());
    }

    @Test
    void citaProgramadaPermiteCancelar() throws Exception {

        var pantalla = crearPantalla((padre, id) -> true);
        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);

        seleccionar(tabla, 0);

        assertTrue(boton.isEnabled());
    }

    @Test
    void citaConfirmadaPermiteCancelar() throws Exception {

        var pantalla = crearPantalla((padre, id) -> true);
        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);

        seleccionar(tabla, 1);

        assertTrue(boton.isEnabled());
    }

    @Test
    void citaCanceladaNoPermiteCancelar() throws Exception {

        var pantalla = crearPantalla((padre, id) -> true);
        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);

        seleccionar(tabla, 2);

        assertFalse(boton.isEnabled());
    }

    @Test
    void citaRealizadaNoPermiteCancelar() throws Exception {

        var pantalla = crearPantalla((padre, id) -> true);
        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);

        seleccionar(tabla, 3);

        assertFalse(boton.isEnabled());
    }

    @Test
    void cambiarSeleccionActualizaElBoton() throws Exception {

        var pantalla = crearPantalla((padre, id) -> true);
        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);

        seleccionar(tabla, 0);
        assertTrue(boton.isEnabled());

        seleccionar(tabla, 2);
        assertFalse(boton.isEnabled());

        SwingUtilities.invokeAndWait(tabla::clearSelection);
        assertFalse(boton.isEnabled());
    }

    @Test
    void aceptarCancelacionActualizaPantalla() throws Exception {

        AtomicInteger dialogos = new AtomicInteger();

        var pantalla = crearPantalla((padre, id) -> {
            dialogos.incrementAndGet();
            return true;
        });

        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);
        JTextArea detalle = buscar(
                pantalla, "detalleCita", JTextArea.class);
        JLabel mensaje = buscar(
                pantalla, "mensajeCitas", JLabel.class);

        SwingUtilities.invokeAndWait(() -> {
            tabla.setRowSelectionInterval(0, 0);
            boton.doClick();
        });

        assertEquals(1, dialogos.get());
        assertEquals(EstadoCita.CANCELADA,
                servicio.consultarMiCita(paciente, 1L).getEstado());
        assertEquals(EstadoCita.CANCELADA, tabla.getValueAt(0, 4));
        assertTrue(detalle.getText().contains("CANCELADA"));
        assertTrue(mensaje.getText()
                .contains("cancelada correctamente"));
        assertFalse(boton.isEnabled());
    }

    @Test
    void cancelarCitaConfirmada() throws Exception {

        var pantalla = crearPantalla((padre, id) -> true);
        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);

        SwingUtilities.invokeAndWait(() -> {
            tabla.setRowSelectionInterval(1, 1);
            boton.doClick();
        });

        assertEquals(EstadoCita.CANCELADA,
                servicio.consultarMiCita(paciente, 2L).getEstado());
    }

    @Test
    void rechazarDialogoNoModificaLaCita() throws Exception {

        AtomicInteger dialogos = new AtomicInteger();

        var pantalla = crearPantalla((padre, id) -> {
            dialogos.incrementAndGet();
            return false;
        });

        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);

        SwingUtilities.invokeAndWait(() -> {
            tabla.setRowSelectionInterval(0, 0);
            boton.doClick();
        });

        assertEquals(1, dialogos.get());
        assertEquals(EstadoCita.PROGRAMADA,
                servicio.consultarMiCita(paciente, 1L).getEstado());
        assertEquals(EstadoCita.PROGRAMADA, tabla.getValueAt(0, 4));
    }

    @Test
    void cancelarNoPideElDialogoDeConfirmar() throws Exception {

        AtomicInteger confirmaciones = new AtomicInteger();

        var pantalla = crearPantalla((padre, id) -> {
            confirmaciones.incrementAndGet();
            return true;
        }, (padre, id) -> true);

        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);

        SwingUtilities.invokeAndWait(() -> {
            tabla.setRowSelectionInterval(0, 0);
            boton.doClick();
        });

        assertEquals(0, confirmaciones.get());
    }

    @Test
    void cancelacionFueraDePlazoMuestraElError() throws Exception {

        servicio.fijarReloj(Clock.fixed(
                CITA.minusHours(2).atZone(ZONA).toInstant(), ZONA));

        var pantalla = crearPantalla((padre, id) -> true);
        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);
        JLabel mensaje = buscar(
                pantalla, "mensajeCitas", JLabel.class);

        SwingUtilities.invokeAndWait(() -> {
            tabla.setRowSelectionInterval(0, 0);
            boton.doClick();
        });

        assertTrue(mensaje.getText().contains("Error al cancelar"));
        assertTrue(mensaje.getText().contains("antelación"));
        assertEquals(EstadoCita.PROGRAMADA,
                servicio.consultarMiCita(paciente, 1L).getEstado());
    }

    @Test
    void citaCanceladaPorOtroMuestraElError() throws Exception {

        var pantalla = crearPantalla((padre, id) -> true);
        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCancelarCita", JButton.class);
        JLabel mensaje = buscar(
                pantalla, "mensajeCitas", JLabel.class);

        seleccionar(tabla, 0);

        //otra pantalla o el personal administrativo cancela la cita
        servicio.cancelarMiCita(paciente, 1L);

        SwingUtilities.invokeAndWait(boton::doClick);

        assertTrue(mensaje.getText().contains("Error al cancelar"));
    }
}
