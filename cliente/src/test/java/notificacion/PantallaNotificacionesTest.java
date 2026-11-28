package notificacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

class PantallaNotificacionesTest {

    private static final Usuario PACIENTE =
            new Usuario("paciente1", Rol.PACIENTE);

    private static Notificacion crearNotificacion(
            long id, String titulo, boolean leida) {
        Notificacion notificacion = new Notificacion();
        notificacion.setId(id);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje("Mensaje " + titulo);
        notificacion.setFechaHora(
                LocalDateTime.of(2026, 11, 28, 10, 0));
        notificacion.setLeida(leida);
        return notificacion;
    }

    private static class ServicioPrueba
            implements NotificacionesService {

        private List<Notificacion> notificaciones = List.of();
        private boolean fallar;
        private int consultas;

        @Override
        public List<Notificacion> listarMisNotificaciones(
                Usuario solicitante)
                throws SinPermisoException {
            consultas++;
            if (fallar) {
                throw new SinPermisoException("Acceso denegado");
            }
            return notificaciones;
        }
    }

    @Test
    void muestraCamposYEstadoDeLasNotificaciones()
            throws Exception {
        ServicioPrueba servicio = new ServicioPrueba();
        servicio.notificaciones = List.of(
                crearNotificacion(1, "Cita programada", false),
                crearNotificacion(2, "Datos actualizados", true));

        PantallaNotificaciones[] pantalla =
                new PantallaNotificaciones[1];
        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaNotificaciones(
                        servicio, PACIENTE));

        JTable tabla = buscar(
                pantalla[0], "tablaNotificaciones", JTable.class);

        assertEquals(2, tabla.getRowCount());
        assertEquals(4, tabla.getColumnCount());
        assertEquals("Cita programada",
                tabla.getValueAt(0, 0));
        assertEquals("Mensaje Cita programada",
                tabla.getValueAt(0, 1));
        assertEquals("No leída", tabla.getValueAt(0, 3));
        assertEquals("Leída", tabla.getValueAt(1, 3));
    }

    @Test
    void muestraEstadoVacio() throws Exception {
        PantallaNotificaciones[] pantalla =
                new PantallaNotificaciones[1];
        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaNotificaciones(
                        new ServicioPrueba(), PACIENTE));

        JTable tabla = buscar(
                pantalla[0], "tablaNotificaciones", JTable.class);
        JLabel mensaje = buscar(
                pantalla[0], "mensajeNotificaciones", JLabel.class);

        assertEquals(0, tabla.getRowCount());
        assertEquals("No tienes notificaciones.", mensaje.getText());
    }

    @Test
    void muestraErrorDelServicio() throws Exception {
        ServicioPrueba servicio = new ServicioPrueba();
        servicio.fallar = true;

        PantallaNotificaciones[] pantalla =
                new PantallaNotificaciones[1];
        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaNotificaciones(
                        servicio, PACIENTE));

        JLabel mensaje = buscar(
                pantalla[0], "mensajeNotificaciones", JLabel.class);

        assertTrue(mensaje.getText().contains("Acceso denegado"));
        assertEquals(1, servicio.consultas);
    }

    @Test
    void botonActualizarVuelveAConsultar()
            throws Exception {
        ServicioPrueba servicio = new ServicioPrueba();
        PantallaNotificaciones[] pantalla =
                new PantallaNotificaciones[1];
        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaNotificaciones(
                        servicio, PACIENTE));

        JButton actualizar = buscar(
                pantalla[0],
                "botonActualizarNotificaciones",
                JButton.class);
        SwingUtilities.invokeAndWait(actualizar::doClick);

        assertEquals(2, servicio.consultas);
    }

    @Test
    void pantallaSoloDisponibleParaPacientes() {
        assertTrue(PantallaNotificaciones.disponiblePara(PACIENTE));
        assertFalse(PantallaNotificaciones.disponiblePara(
                new Usuario("medico", Rol.MEDICO)));
        assertFalse(PantallaNotificaciones.disponiblePara(null));
    }

    @Test
    void noConsultaServicioSiUsuarioNoEsPaciente()
            throws Exception {
        ServicioPrueba servicio = new ServicioPrueba();
        Usuario medico = new Usuario("medico", Rol.MEDICO);

        PantallaNotificaciones[] pantalla =
                new PantallaNotificaciones[1];
        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaNotificaciones(
                        servicio, medico));

        JLabel mensaje = buscar(
                pantalla[0], "mensajeNotificaciones", JLabel.class);

        assertEquals(0, servicio.consultas);
        assertTrue(mensaje.getText().contains("solo está disponible"));
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
}
