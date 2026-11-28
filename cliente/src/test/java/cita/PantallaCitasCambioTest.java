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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU03 - Pruebas del botón Cambiar cita en Swing.
 * El diálogo de elección del hueco se simula y no se abren ventanas.
 */
class PantallaCitasCambioTest {

    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final LocalDateTime AHORA =
            LocalDateTime.of(2026, 12, 1, 9, 0);
    private static final LocalDateTime CITA =
            LocalDateTime.of(2026, 12, 10, 9, 0);
    private static final LocalDateTime NUEVA =
            LocalDateTime.of(2026, 12, 10, 10, 0);

    private Usuario paciente;
    private CitasServiceSimulado servicio;

    @BeforeEach
    void preparar() {

        paciente = new Usuario("paciente1", Rol.PACIENTE);

        servicio = new CitasServiceSimulado();
        servicio.vincularPaciente("paciente1", 10L);

        servicio.agregarHuecoLibre(
                new HuecoLibre(100L, 7L, CITA));
        servicio.agregarHuecoLibre(
                new HuecoLibre(200L, 8L, NUEVA));

        //filas 0 a 3: ids 1 a 4
        servicio.agregarCita(crearCita(EstadoCita.PROGRAMADA, 100L));
        servicio.agregarCita(crearCita(EstadoCita.CONFIRMADA, null));
        servicio.agregarCita(crearCita(EstadoCita.CANCELADA, null));
        servicio.agregarCita(crearCita(EstadoCita.REALIZADA, null));

        servicio.fijarReloj(Clock.fixed(
                AHORA.atZone(ZONA).toInstant(), ZONA));
    }

    private Cita crearCita(EstadoCita estado, Long huecoId) {

        Cita cita = new Cita();
        cita.setPacienteId(10L);
        cita.setMedicoId(7L);
        cita.setTipoPruebaId(30L);
        cita.setFechaHora(CITA);
        cita.setEstado(estado);
        cita.setHuecoId(huecoId);

        return cita;
    }

    private PantallaCitasPaciente crearPantalla(
            CitasService servicioUsado,
            PantallaCitasPaciente.ElectorHueco elector)
            throws Exception {

        PantallaCitasPaciente[] resultado =
                new PantallaCitasPaciente[1];

        SwingUtilities.invokeAndWait(() ->
                resultado[0] = new PantallaCitasPaciente(
                        servicioUsado, paciente, elector));

        return resultado[0];
    }

    private PantallaCitasPaciente crearPantalla(
            PantallaCitasPaciente.ElectorHueco elector)
            throws Exception {

        return crearPantalla(servicio, elector);
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

    private void cambiarFila(
            PantallaCitasPaciente pantalla, int fila)
            throws Exception {

        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCambiarCita", JButton.class);

        SwingUtilities.invokeAndWait(() -> {
            tabla.setRowSelectionInterval(fila, fila);
            boton.doClick();
        });
    }

    @Test
    void botonDesactivadoSinSeleccion() throws Exception {

        var pantalla = crearPantalla((padre, id, huecos) -> null);

        JButton boton = buscar(
                pantalla, "botonCambiarCita", JButton.class);

        assertNotNull(boton);
        assertFalse(boton.isEnabled());
    }

    @Test
    void citasActivasPermitenCambiar() throws Exception {

        var pantalla = crearPantalla((padre, id, huecos) -> null);
        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCambiarCita", JButton.class);

        seleccionar(tabla, 0);
        assertTrue(boton.isEnabled());

        seleccionar(tabla, 1);
        assertTrue(boton.isEnabled());
    }

    @Test
    void citasCerradasNoPermitenCambiar() throws Exception {

        var pantalla = crearPantalla((padre, id, huecos) -> null);
        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCambiarCita", JButton.class);

        seleccionar(tabla, 2);
        assertFalse(boton.isEnabled());

        seleccionar(tabla, 3);
        assertFalse(boton.isEnabled());

        SwingUtilities.invokeAndWait(tabla::clearSelection);
        assertFalse(boton.isEnabled());
    }

    @Test
    void elegirHuecoCambiaLaCita() throws Exception {

        List<HuecoLibre> recibidos = new ArrayList<>();
        AtomicInteger dialogos = new AtomicInteger();

        var pantalla = crearPantalla((padre, id, huecos) -> {
            dialogos.incrementAndGet();
            recibidos.addAll(huecos);
            assertEquals(1L, id);
            return huecos.get(0);
        });

        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JButton boton = buscar(
                pantalla, "botonCambiarCita", JButton.class);
        JTextArea detalle = buscar(
                pantalla, "detalleCita", JTextArea.class);
        JLabel mensaje = buscar(
                pantalla, "mensajeCitas", JLabel.class);

        cambiarFila(pantalla, 0);

        assertEquals(1, dialogos.get());
        //solo se ofrece el hueco libre, no el de la cita actual
        assertEquals(List.of(200L),
                recibidos.stream().map(HuecoLibre::id).toList());

        Cita cambiada = servicio.consultarMiCita(paciente, 1L);

        assertEquals(200L, cambiada.getHuecoId());
        assertEquals(NUEVA, cambiada.getFechaHora());
        assertEquals(NUEVA, tabla.getValueAt(0, 1));
        assertEquals(EstadoCita.PROGRAMADA, tabla.getValueAt(0, 4));
        assertTrue(detalle.getText().contains("PROGRAMADA"));
        assertTrue(mensaje.getText()
                .contains("cambiada correctamente"));
        assertTrue(boton.isEnabled());
    }

    @Test
    void cambiarCitaConfirmadaLaDejaProgramada() throws Exception {

        var pantalla = crearPantalla(
                (padre, id, huecos) -> huecos.get(0));
        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);

        cambiarFila(pantalla, 1);

        assertEquals(EstadoCita.PROGRAMADA, tabla.getValueAt(1, 4));
    }

    @Test
    void noElegirHuecoNoModificaLaCita() throws Exception {

        AtomicInteger dialogos = new AtomicInteger();

        var pantalla = crearPantalla((padre, id, huecos) -> {
            dialogos.incrementAndGet();
            return null;
        });

        JTable tabla = buscar(pantalla, "tablaCitas", JTable.class);
        JLabel mensaje = buscar(
                pantalla, "mensajeCitas", JLabel.class);

        cambiarFila(pantalla, 0);

        assertEquals(1, dialogos.get());
        assertEquals(100L,
                servicio.consultarMiCita(paciente, 1L).getHuecoId());
        assertEquals(CITA, tabla.getValueAt(0, 1));
        assertEquals("Cambio cancelado", mensaje.getText());
    }

    @Test
    void sinHuecosLibresNoSeAbreElDialogo() throws Exception {

        //el único hueco libre pasa a estar ocupado
        servicio.agregarCita(crearCita(EstadoCita.PROGRAMADA, 200L));

        AtomicInteger dialogos = new AtomicInteger();

        var pantalla = crearPantalla((padre, id, huecos) -> {
            dialogos.incrementAndGet();
            return null;
        });

        JLabel mensaje = buscar(
                pantalla, "mensajeCitas", JLabel.class);

        cambiarFila(pantalla, 0);

        assertEquals(0, dialogos.get());
        assertTrue(mensaje.getText().contains("No hay huecos libres"));
    }

    @Test
    void cambioFueraDePoliticaMuestraElError() throws Exception {

        //faltan 2 horas para la cita: dentro del plazo de cambio
        servicio.fijarReloj(Clock.fixed(
                CITA.minusHours(2).atZone(ZONA).toInstant(), ZONA));

        var pantalla = crearPantalla(
                (padre, id, huecos) -> huecos.get(0));
        JLabel mensaje = buscar(
                pantalla, "mensajeCitas", JLabel.class);

        cambiarFila(pantalla, 0);

        assertTrue(mensaje.getText().contains("Error al cambiar"));
        assertTrue(mensaje.getText().contains("antelación"));
        assertEquals(100L,
                servicio.consultarMiCita(paciente, 1L).getHuecoId());
    }

    @Test
    void huecoOcupadoMientrasSeEligeMuestraElError() throws Exception {

        var pantalla = crearPantalla((padre, id, huecos) -> {
            //otro paciente ocupa el hueco mientras se elige
            servicio.agregarCita(
                    crearCita(EstadoCita.PROGRAMADA, 200L));
            return huecos.get(0);
        });

        JLabel mensaje = buscar(
                pantalla, "mensajeCitas", JLabel.class);

        cambiarFila(pantalla, 0);

        assertTrue(mensaje.getText().contains("Error al cambiar"));
        assertEquals(100L,
                servicio.consultarMiCita(paciente, 1L).getHuecoId());
    }

    @Test
    void errorAlBuscarHuecosSeMuestra() throws Exception {

        CitasServiceSimulado fallido = new CitasServiceSimulado() {
            @Override
            public List<HuecoLibre> listarHuecosLibres(
                    Usuario solicitante)
                    throws SinPermisoException {
                throw new SinPermisoException("Sin permiso");
            }
        };

        fallido.vincularPaciente("paciente1", 10L);
        fallido.agregarCita(crearCita(EstadoCita.PROGRAMADA, 100L));

        AtomicInteger dialogos = new AtomicInteger();

        var pantalla = crearPantalla(fallido, (padre, id, huecos) -> {
            dialogos.incrementAndGet();
            return null;
        });

        JLabel mensaje = buscar(
                pantalla, "mensajeCitas", JLabel.class);

        cambiarFila(pantalla, 0);

        assertEquals(0, dialogos.get());
        assertTrue(mensaje.getText()
                .contains("Error al buscar huecos"));
    }
}
