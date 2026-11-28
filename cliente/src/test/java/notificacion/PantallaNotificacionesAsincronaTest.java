package notificacion;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

class PantallaNotificacionesAsincronaTest {

    private static class ServicioLento
            implements NotificacionesService {

        private final CountDownLatch inicio =
                new CountDownLatch(1);
        private final CountDownLatch liberar =
                new CountDownLatch(1);
        private final AtomicBoolean ejecutadoEnEdt =
                new AtomicBoolean(true);

        @Override
        public List<Notificacion> listarMisNotificaciones(
                Usuario solicitante) {
            ejecutadoEnEdt.set(
                    SwingUtilities.isEventDispatchThread());
            inicio.countDown();
            try {
                if (!liberar.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "La espera ha superado el límite");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
            return List.of();
        }
    }

    @Test
    void cargaAsincronaNoBloqueaSwing() throws Exception {
        ServicioLento servicio = new ServicioLento();

        try {
            SwingUtilities.invokeAndWait(() ->
                    new PantallaNotificaciones(
                            servicio,
                            new Usuario("paciente-demo", Rol.PACIENTE),
                            true));

            assertTrue(servicio.inicio.await(
                    5, TimeUnit.SECONDS));
            assertFalse(servicio.ejecutadoEnEdt.get());

            CountDownLatch eventoSwing = new CountDownLatch(1);
            SwingUtilities.invokeLater(eventoSwing::countDown);
            assertTrue(
                    eventoSwing.await(2, TimeUnit.SECONDS),
                    "La interfaz Swing se ha bloqueado");
        } finally {
            servicio.liberar.countDown();
        }
    }
}
