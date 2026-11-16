package paciente;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PantallasPacienteConstruccionTest {

    @BeforeEach
    void comprobarEntornoGrafico() {
        assumeFalse(
                GraphicsEnvironment.isHeadless(),
                "El entorno es headless");
    }

    @Test
    @DisplayName(
            "La pantalla de alta de pacientes se construye correctamente")
    void construirPantallaAlta()
            throws Exception {

        AtomicReference<PantallaAltaPaciente> ref =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {

            Usuario usuario =
                    new Usuario(
                            "personal",
                            Rol.PERSONAL_ADMINISTRATIVO);

            ref.set(
                    new PantallaAltaPaciente(
                            new PacientesServiceSimulado(),
                            usuario));

            assertNotNull(
                    ref.get().getContentPane());
        });

        SwingUtilities.invokeAndWait(
                () -> ref.get().dispose());
    }

    @Test
    @DisplayName(
            "La pantalla de modificación de pacientes se construye correctamente")
    void construirPantallaModificar()
            throws Exception {

        AtomicReference<PantallaModificarPaciente> ref =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {

            Usuario usuario =
                    new Usuario(
                            "personal",
                            Rol.PERSONAL_ADMINISTRATIVO);

            ref.set(
                    new PantallaModificarPaciente(
                            new PacientesServiceSimulado(),
                            usuario));

            assertNotNull(
                    ref.get().getContentPane());
        });

        SwingUtilities.invokeAndWait(
                () -> ref.get().dispose());
    }
}