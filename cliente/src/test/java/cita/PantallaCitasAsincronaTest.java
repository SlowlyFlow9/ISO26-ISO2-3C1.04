package cita;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU02 - Verifica que la carga asincrona no bloquea el hilo grafico de Swing.
 */

class PantallaCitasAsincronaTest {

    private static class ServicioLento
            implements CitasService {

        final CountDownLatch inicio =
                new CountDownLatch(1);

        final CountDownLatch liberar =
                new CountDownLatch(1);

        final AtomicBoolean ejecutadoEnEdt =
                new AtomicBoolean(true);

        @Override
        public List<Cita> listarMisCitas(
                Usuario usuario) {

            ejecutadoEnEdt.set(
                    SwingUtilities.isEventDispatchThread());

            inicio.countDown();

            try {
                if (!liberar.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "La espera ha superado el limite");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }

            return List.of();
        }

        @Override
        public Cita consultarMiCita(
                Usuario usuario, long citaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cita confirmarMiCita(
                Usuario usuario, long citaId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Cita cancelarMiCita(
                Usuario usuario, long citaId) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void cargaAsincronaNoBloqueaSwing()
            throws Exception {

        ServicioLento servicio = new ServicioLento();

        try {
            SwingUtilities.invokeAndWait(() -> {

                new PantallaCitasPaciente(
                        servicio,
                        new Usuario(
                                "paciente-demo",
                                Rol.PACIENTE),
                        true);
            });

            // Comprobar que ha comenzado la consulta.
            assertTrue(
                    servicio.inicio.await(
                            5, TimeUnit.SECONDS));

            // La consulta no debe ejecutarse
            // en el hilo grafico de Swing.
            assertFalse(
                    servicio.ejecutadoEnEdt.get());

            // Swing debe seguir procesando eventos
            // mientras el servicio esta esperando.
            CountDownLatch eventoSwing =
                    new CountDownLatch(1);

            SwingUtilities.invokeLater(
                    eventoSwing::countDown);

            assertTrue(
                    eventoSwing.await(
                            2, TimeUnit.SECONDS),
                    "La interfaz Swing se ha bloqueado");

        } finally {
            servicio.liberar.countDown();
        }
    }
}
