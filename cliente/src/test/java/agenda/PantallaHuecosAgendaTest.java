package agenda;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;

import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU29 - Pruebas de la pantalla Swing.
 */


public class PantallaHuecosAgendaTest {

    private static final Usuario ADMINISTRATIVO =
            new Usuario(
                    "Administrativo",
                    Rol.PERSONAL_ADMINISTRATIVO);

    // Servicio falso para probar la pantalla.
    private static class ServicioFalso
            implements HuecosAgendaService {

        private long ultimaAgendaConsultada;

        @Override
        public HuecoAgenda crear(
                Usuario usuario, HuecoAgenda datos) {
            return datos;
        }

        @Override
        public List<HuecoAgenda> listarPorAgenda(
                Usuario usuario, long agendaId) {

            ultimaAgendaConsultada = agendaId;

            HuecoAgenda primero = new HuecoAgenda();
            primero.setId(1L);
            primero.setAgendaId(agendaId);
            primero.setHoraInicio(LocalTime.of(9, 0));
            primero.setHoraFin(LocalTime.of(9, 30));
            primero.setBloqueado(false);

            HuecoAgenda segundo = new HuecoAgenda();
            segundo.setId(2L);
            segundo.setAgendaId(agendaId);
            segundo.setHoraInicio(LocalTime.of(10, 0));
            segundo.setHoraFin(LocalTime.of(10, 30));
            segundo.setBloqueado(true);

            return List.of(primero, segundo);
        }

        @Override
        public HuecoAgenda bloquear(
                Usuario usuario, long id) {
            return new HuecoAgenda();
        }

        @Override
        public HuecoAgenda liberar(
                Usuario usuario, long id) {
            return new HuecoAgenda();
        }
    }

    // Busca un botón dentro de los componentes Swing.
    private JButton buscarBoton(
            Container contenedor, String nombre) {

        for (Component componente :
                contenedor.getComponents()) {

            if (componente instanceof JButton boton
                    && nombre.equals(boton.getText())) {
                return boton;
            }

            if (componente instanceof Container hijo) {

                JButton encontrado =
                        buscarBoton(hijo, nombre);

                if (encontrado != null) {
                    return encontrado;
                }
            }
        }

        return null;
    }

    // Busca la tabla de huecos.
    private JTable buscarTabla(Container contenedor) {

        for (Component componente :
                contenedor.getComponents()) {

            if (componente instanceof JTable tabla) {
                return tabla;
            }

            if (componente instanceof Container hijo) {

                JTable encontrada = buscarTabla(hijo);

                if (encontrada != null) {
                    return encontrada;
                }
            }
        }

        return null;
    }

    // Busca los campos de texto del formulario.
    private List<JTextField> buscarCampos(
            Container contenedor) {

        List<JTextField> resultado = new ArrayList<>();

        for (Component componente :
                contenedor.getComponents()) {

            if (componente instanceof JTextField campo) {
                resultado.add(campo);
            }

            if (componente instanceof Container hijo) {
                resultado.addAll(buscarCampos(hijo));
            }
        }

        return resultado;
    }

    @Test
    void rechazarServicioNulo() {

        assertThrows(IllegalArgumentException.class,
                () -> new PantallaHuecosAgenda(
                        null, ADMINISTRATIVO));
    }

    @Test
    void construirPantallaConComponentes()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            PantallaHuecosAgenda pantalla =
                    new PantallaHuecosAgenda(
                            new ServicioFalso(),
                            ADMINISTRATIVO);

            assertNotNull(buscarTabla(pantalla));

            assertEquals(3,
                    buscarCampos(pantalla).size());

            assertNotNull(
                    buscarBoton(pantalla, "Consultar"));

            assertNotNull(
                    buscarBoton(pantalla, "Crear hueco"));

            assertNotNull(
                    buscarBoton(pantalla, "Bloquear"));

            assertNotNull(
                    buscarBoton(pantalla, "Liberar"));
        });
    }

    @Test
    void deshabilitarAccionesSinPermiso()
            throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            Usuario medico =
                    new Usuario("Medico", Rol.MEDICO);

            PantallaHuecosAgenda pantalla =
                    new PantallaHuecosAgenda(
                            new ServicioFalso(), medico);

            assertFalse(
                    buscarBoton(pantalla,
                            "Consultar").isEnabled());

            assertFalse(
                    buscarBoton(pantalla,
                            "Crear hueco").isEnabled());

            assertFalse(
                    buscarBoton(pantalla,
                            "Bloquear").isEnabled());

            assertFalse(
                    buscarBoton(pantalla,
                            "Liberar").isEnabled());
        });
    }

    @Test
    void consultarYMostrarHuecos()
            throws Exception {

        ServicioFalso servicio = new ServicioFalso();

        CountDownLatch tablaActualizada =
                new CountDownLatch(1);

        AtomicReference<PantallaHuecosAgenda> referencia =
                new AtomicReference<>();

        // Preparar y pulsar Consultar en el hilo Swing.
        SwingUtilities.invokeAndWait(() -> {

            PantallaHuecosAgenda pantalla =
                    new PantallaHuecosAgenda(
                            servicio, ADMINISTRATIVO);

            referencia.set(pantalla);

            JTable tabla = buscarTabla(pantalla);

            tabla.getModel().addTableModelListener(
                    evento -> {
                        if (evento.getType()
                                == TableModelEvent.INSERT) {
                            tablaActualizada.countDown();
                        }
                    });

            // El primer campo corresponde al ID de agenda.
            buscarCampos(pantalla)
                    .get(0)
                    .setText("4");

            buscarBoton(pantalla, "Consultar")
                    .doClick();
        });

        // Esperar a que SwingWorker actualice la tabla.
        assertTrue(
                tablaActualizada.await(
                        5, TimeUnit.SECONDS),
                "La tabla no se ha actualizado");

        SwingUtilities.invokeAndWait(() -> {

            JTable tabla = buscarTabla(referencia.get());

            assertEquals(4L,
                    servicio.ultimaAgendaConsultada);

            assertEquals(2, tabla.getRowCount());

            assertEquals("Disponible",
                    tabla.getValueAt(0, 3));

            assertEquals("Bloqueado",
                    tabla.getValueAt(1, 3));
        });
    }


}
