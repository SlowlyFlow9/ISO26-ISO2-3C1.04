package cita;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

class PantallaIncidenciasCitaTest {

    private static final Usuario MEDICO =
            new Usuario("medico", Rol.MEDICO, 30L);

    private static final class ServicioFalso
            implements CitasIncidenciasService {

        private long citaRecibida;
        private String descripcionRecibida;
        private int consultas;

        @Override
        public IncidenciaCita registrarIncidencia(
                Usuario solicitante,
                long citaId,
                String descripcion) {
            citaRecibida = citaId;
            descripcionRecibida = descripcion;
            return incidencia(citaId, descripcion);
        }

        @Override
        public List<IncidenciaCita> listarIncidencias(
                Usuario solicitante,
                long citaId) {
            consultas++;
            return List.of(
                    incidencia(citaId, "Dolor"),
                    incidencia(citaId, "Reacción"));
        }

        private IncidenciaCita incidencia(
                long citaId,
                String descripcion) {
            IncidenciaCita incidencia = new IncidenciaCita();
            incidencia.setId(1L);
            incidencia.setCitaId(citaId);
            incidencia.setMedicoId(30L);
            incidencia.setFechaHora(
                    LocalDateTime.of(2026, 11, 27, 17, 0));
            incidencia.setDescripcion(descripcion);
            return incidencia;
        }
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
        assertTrue(PantallaIncidenciasCita.disponiblePara(MEDICO));
        assertFalse(PantallaIncidenciasCita.disponiblePara(
                new Usuario("paciente", Rol.PACIENTE, 30L)));
        assertFalse(PantallaIncidenciasCita.disponiblePara(
                new Usuario("medico", Rol.MEDICO)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PantallaIncidenciasCita(
                        new ServicioFalso(),
                        new Usuario("paciente", Rol.PACIENTE)));
    }

    @Test
    void registraIncidenciaYActualizaHistorial()
            throws Exception {
        ServicioFalso servicio = new ServicioFalso();
        AtomicReference<PantallaIncidenciasCita> referencia =
                new AtomicReference<>();
        SwingUtilities.invokeAndWait(() ->
                referencia.set(new PantallaIncidenciasCita(
                        servicio, MEDICO)));

        PantallaIncidenciasCita pantalla = referencia.get();
        JTextField campoId = buscarComponente(
                pantalla, "campoCitaId", JTextField.class);
        JTextArea campoDescripcion = buscarComponente(
                pantalla, "campoDescripcion", JTextArea.class);
        JTable tabla = buscarComponente(
                pantalla, "tablaIncidencias", JTable.class);

        assertNotNull(campoId);
        assertNotNull(campoDescripcion);
        assertNotNull(tabla);

        SwingUtilities.invokeAndWait(() -> {
            campoId.setText("12");
            campoDescripcion.setText("Dolor");
            buscarComponente(
                    pantalla,
                    "botonRegistrarIncidencia",
                    JButton.class).doClick();
        });

        assertEquals(12L, servicio.citaRecibida);
        assertEquals("Dolor", servicio.descripcionRecibida);
        assertEquals(1, servicio.consultas);
        assertEquals(2, tabla.getRowCount());
        assertEquals("Dolor", tabla.getValueAt(0, 1));
        assertNotNull(buscarComponente(
                pantalla, "mensaje", JLabel.class));
    }
}
