package solicitud;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;
import tipoprueba.TipoPrueba;

class PantallaPrescribirPruebaConstruccionTest {

    @BeforeEach
    void comprobarEntornoGrafico() {
        assumeFalse(
                GraphicsEnvironment.isHeadless(),
                "El entorno es headless");
    }

    @Test
    @DisplayName(
            "La pantalla de prescripción se construye correctamente")
    void construirPantallaPrescripcion()
            throws Exception {

        AtomicReference<PantallaPrescribirPrueba> ref =
                new AtomicReference<>();

        Usuario medico =
                new Usuario(
                        "medico",
                        Rol.MEDICO);

        SolicitudesService servicio =
                new SolicitudesService() {

                    @Override
                    public List<TipoPrueba> consultarTiposPrueba(
                            Usuario usuario) {

                        return List.of(
                                new TipoPrueba(
                                        1L,
                                        "Radiografía",
                                        "Prueba diagnóstica",
                                        15,
                                        false,
                                        "",
                                        true),
                                new TipoPrueba(
                                        2L,
                                        "Prueba inactiva",
                                        "No disponible",
                                        20,
                                        false,
                                        "",
                                        false));
                    }

                    @Override
                    public SolicitudPrueba crearSolicitud(
                            Usuario usuario,
                            SolicitudPrueba datos) {

                        return datos;
                    }

                    @Override
                    public SolicitudPrueba consultarSolicitud(
                            Usuario usuario,
                            long id) {

                        return null;
                    }
                };

        SwingUtilities.invokeAndWait(() -> {

            ref.set(
                    new PantallaPrescribirPrueba(
                            servicio,
                            medico));

            assertNotNull(
                    ref.get().getContentPane());
        });

        Thread.sleep(300);

        SwingUtilities.invokeAndWait(
                () -> assertNotNull(ref.get()));

        SwingUtilities.invokeAndWait(
                () -> ref.get().dispose());
    }
}