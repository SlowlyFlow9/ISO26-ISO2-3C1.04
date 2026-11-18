package protocolo;

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

class PantallaProtocolosPruebaConstruccionTest {

    @BeforeEach
    void comprobarEntornoGrafico() {
        assumeFalse(
                GraphicsEnvironment.isHeadless(),
                "El entorno es headless");
    }

    @Test
    @DisplayName(
            "La pantalla de protocolos se construye correctamente")
    void construirPantallaProtocolos()
            throws Exception {

        AtomicReference<PantallaProtocolosPrueba> ref =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {

            Usuario admin =
                    new Usuario(
                            "admin",
                            Rol.ADMINISTRADOR);

            ProtocolosPruebaServiceSimulado servicio =
                    new ProtocolosPruebaServiceSimulado(
                            List.of(1L, 2L));

            List<TipoPruebaOpcion> tipos =
                    List.of(
                            new TipoPruebaOpcion(
                                    1L,
                                    "Radiografía",
                                    true),
                            new TipoPruebaOpcion(
                                    2L,
                                    "TAC",
                                    true));

            ref.set(
                    new PantallaProtocolosPrueba(
                            servicio,
                            admin,
                            tipos));

            assertNotNull(
                    ref.get().getContentPane());
        });

        SwingUtilities.invokeAndWait(
                () -> ref.get().dispose());
    }
}