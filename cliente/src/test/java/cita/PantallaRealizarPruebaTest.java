package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

class PantallaRealizarPruebaTest {

    private static final Usuario MEDICO =
            new Usuario("medico", Rol.MEDICO, 30L);

    private static final class ServicioFalso
            implements CitasRealizacionService {

        private long citaRecibida;

        @Override
        public Cita marcarRealizada(
                Usuario solicitante,
                long citaId) {
            citaRecibida = citaId;
            Cita cita = new Cita();
            cita.setId(citaId);
            cita.setEstado(EstadoCita.REALIZADA);
            return cita;
        }
    }

    private JButton buscarBoton(
            Container contenedor,
            String nombre) {
        for (Component componente : contenedor.getComponents()) {
            if (componente instanceof JButton boton
                    && nombre.equals(boton.getName())) {
                return boton;
            }
            if (componente instanceof Container hijo) {
                JButton encontrado = buscarBoton(hijo, nombre);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    private <T extends Component> T buscarComponente(
            Container contenedor,
            String nombre,
            Class<T> tipo) {
        for (Component componente : contenedor.getComponents()) {
            if (tipo.isInstance(componente)
                    && nombre.equals(componente.getName())) {
                return tipo.cast(componente);
            }
            if (componente instanceof Container hijo) {
                T encontrado = buscarComponente(hijo, nombre, tipo);
                if (encontrado != null) {
                    return encontrado;
                }
            }
        }
        return null;
    }

    @Test
    void soloPermiteAccesoAMedicosIdentificados() {
        assertTrue(PantallaRealizarPrueba.disponiblePara(MEDICO));
        assertFalse(PantallaRealizarPrueba.disponiblePara(
                new Usuario("paciente", Rol.PACIENTE, 30L)));
        assertFalse(PantallaRealizarPrueba.disponiblePara(
                new Usuario("medico", Rol.MEDICO)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PantallaRealizarPrueba(
                        new ServicioFalso(),
                        new Usuario("paciente", Rol.PACIENTE)));
    }

    @Test
    void marcarUsaElIdentificadorIntroducido()
            throws Exception {
        ServicioFalso servicio = new ServicioFalso();
        AtomicReference<PantallaRealizarPrueba> referencia =
                new AtomicReference<>();

        SwingUtilities.invokeAndWait(() ->
                referencia.set(new PantallaRealizarPrueba(
                        servicio, MEDICO)));

        PantallaRealizarPrueba pantalla = referencia.get();
        JTextField campoId = buscarComponente(
                pantalla, "campoCitaId", JTextField.class);
        assertNotNull(campoId);

        SwingUtilities.invokeAndWait(() -> {
            campoId.setText("12");
            buscarBoton(pantalla, "botonMarcarRealizada").doClick();
        });

        assertEquals(12L, servicio.citaRecibida);
        assertNotNull(buscarComponente(
                pantalla, "mensaje", JLabel.class));
    }
}
