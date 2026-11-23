package solicitud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static soporte.Dialogos.campo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

class PantallaListaEsperaTest {

    private static final Usuario ADMINISTRATIVO =
            new Usuario("administrativo", Rol.PERSONAL_ADMINISTRATIVO);

    @SuppressWarnings("unchecked")
    private static JComboBox<Prioridad> filtroPrioridad(
            PantallaListaEspera pantalla) {
        return campo(pantalla, "filtroPrioridad", JComboBox.class);
    }

    private static class ServicioFalso implements ListaEsperaService {
        private final CountDownLatch permitirPrimeraRespuesta =
                new CountDownLatch(1);
        private volatile Prioridad prioridadRecibida;
        private int consultas;

        @Override
        public List<EntradaListaEspera> consultar(
                Usuario solicitante,
                Prioridad prioridad) {
            prioridadRecibida = prioridad;
            if (consultas++ == 0) {
                try {
                    permitirPrimeraRespuesta.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(
                            "Consulta interrumpida", e);
                }
            }

            List<EntradaListaEspera> entradas = List.of(
                    entrada(1, Prioridad.URGENTE),
                    entrada(2, Prioridad.ORDINARIA));
            return prioridad == null
                    ? entradas
                    : entradas.stream()
                            .filter(e -> e.prioridad() == prioridad)
                            .toList();
        }

        private static EntradaListaEspera entrada(
                long id, Prioridad prioridad) {
            return new EntradaListaEspera(
                    id, 10, 3, prioridad,
                    LocalDateTime.of(2026, 11, 20, 10, 30));
        }
    }

    @Test
    @DisplayName("Solo el personal administrativo puede utilizar la pantalla")
    void validaRol() throws Exception {
        assertTrue(PantallaListaEspera.disponiblePara(ADMINISTRATIVO));
        assertFalse(PantallaListaEspera.disponiblePara(
                new Usuario("medico", Rol.MEDICO)));

        SwingUtilities.invokeAndWait(() -> {
            PantallaListaEspera pantalla = new PantallaListaEspera(
                    new ServicioFalso(),
                    new Usuario("medico", Rol.MEDICO));
            assertFalse(campo(
                    pantalla, "botonConsultar", JButton.class).isEnabled());
        });
    }

    @Test
    @DisplayName("Al consultar carga la lista y aplica el filtro de prioridad")
    void consultaYFiltraLaLista() throws Exception {
        ServicioFalso servicio = new ServicioFalso();
        CountDownLatch primeraActualizacion = new CountDownLatch(1);
        AtomicReference<CountDownLatch> actualizacionEsperada =
                new AtomicReference<>(primeraActualizacion);
        AtomicReference<PantallaListaEspera> referencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            PantallaListaEspera pantalla = new PantallaListaEspera(
                    servicio, ADMINISTRATIVO);
            referencia.set(pantalla);
            ModeloTablaListaEspera modelo =
                    campo(pantalla, "modelo", ModeloTablaListaEspera.class);
            modelo.addTableModelListener(evento -> {
                if (evento.getType() == TableModelEvent.UPDATE
                        || evento.getType() == TableModelEvent.INSERT
                        || evento.getType() == TableModelEvent.DELETE) {
                    actualizacionEsperada.get().countDown();
                }
            });
        });

        servicio.permitirPrimeraRespuesta.countDown();
        assertTrue(primeraActualizacion.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() ->
                assertEquals(2, campo(
                        referencia.get(), "tabla", JTable.class).getRowCount()));

        CountDownLatch segundaActualizacion = new CountDownLatch(1);
        actualizacionEsperada.set(segundaActualizacion);
        SwingUtilities.invokeAndWait(() -> {
            filtroPrioridad(referencia.get())
                    .setSelectedItem(Prioridad.URGENTE);
            campo(referencia.get(), "botonConsultar",
                    JButton.class).doClick();
        });

        assertTrue(segundaActualizacion.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, campo(
                    referencia.get(), "tabla", JTable.class).getRowCount());
            assertEquals(Prioridad.URGENTE, servicio.prioridadRecibida);
        });
    }
}
