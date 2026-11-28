package solicitud;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;

class PantallaReglasPrioridadTest {

    private static final Usuario ADMINISTRADOR =
            new Usuario("admin", Rol.ADMINISTRADOR);

    private static class ServicioFalso
            implements ReglasPrioridadService {
        private final List<ReglaPrioridad> reglas =
                new ArrayList<>();
        private long siguienteId = 1;
        private int listados;
        private int creaciones;
        private int modificaciones;
        private int desactivaciones;
        private boolean fallarListado;

        @Override
        public ReglaPrioridad crear(
                Usuario solicitante,
                DatosReglaPrioridad datos) {
            creaciones++;
            ReglaPrioridad creada = new ReglaPrioridad(
                    siguienteId++,
                    datos.getNombre(),
                    datos.getDescripcion(),
                    datos.getOrden(),
                    true,
                    solicitante.nombre(),
                    LocalDateTime.now());
            reglas.add(creada);
            return creada;
        }

        @Override
        public List<ReglaPrioridad> listar(Usuario solicitante) {
            listados++;
            if (fallarListado) {
                throw new IllegalArgumentException("Error simulado");
            }
            return new ArrayList<>(reglas);
        }

        @Override
        public ReglaPrioridad consultar(Usuario solicitante, long id) {
            return obtener(id);
        }

        @Override
        public ReglaPrioridad modificar(
                Usuario solicitante,
                long id,
                DatosReglaPrioridad datos) {
            modificaciones++;
            ReglaPrioridad actual = obtener(id);
            ReglaPrioridad modificada = new ReglaPrioridad(
                    id,
                    datos.getNombre(),
                    datos.getDescripcion(),
                    datos.getOrden(),
                    actual.isActivo(),
                    solicitante.nombre(),
                    LocalDateTime.now());
            reglas.set(reglas.indexOf(actual), modificada);
            return modificada;
        }

        @Override
        public void desactivar(Usuario solicitante, long id) {
            desactivaciones++;
            ReglaPrioridad actual = obtener(id);
            reglas.set(reglas.indexOf(actual), new ReglaPrioridad(
                    id,
                    actual.getNombre(),
                    actual.getDescripcion(),
                    actual.getOrden(),
                    false,
                    solicitante.nombre(),
                    LocalDateTime.now()));
        }

        private ReglaPrioridad obtener(long id) {
            return reglas.stream()
                    .filter(regla -> regla.getId() == id)
                    .findFirst()
                    .orElseThrow(NoSuchElementException::new);
        }

        private void agregar(ReglaPrioridad regla) {
            reglas.add(regla);
            siguienteId = Math.max(siguienteId, regla.getId() + 1);
        }
    }

    private static class PantallaPrueba
            extends PantallaReglasPrioridad {
        private boolean confirmar = true;

        PantallaPrueba(
                ReglasPrioridadService servicio,
                Usuario usuario) {
            super(servicio, usuario);
        }

        @Override
        protected boolean confirmarDesactivacion(
                ReglaPrioridad regla) {
            return confirmar;
        }
    }

    private static ReglaPrioridad regla(
            long id,
            String nombre,
            int orden,
            boolean activo) {
        return new ReglaPrioridad(
                id,
                nombre,
                "Descripción",
                orden,
                activo,
                "admin",
                LocalDateTime.of(2026, 11, 28, 12, 0));
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

    @Test
    void cargaReglasYRestringeLosBotonesPorEstado() throws Exception {
        ServicioFalso servicio = new ServicioFalso();
        servicio.agregar(regla(1, "Urgente", 1, true));
        servicio.agregar(regla(2, "Inactiva", 2, false));
        PantallaPrueba[] pantalla = new PantallaPrueba[1];

        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaPrueba(
                        servicio, ADMINISTRADOR));

        SwingUtilities.invokeAndWait(() -> {
            JTable tabla = buscar(
                    pantalla[0], "tabla", JTable.class);
            assertEquals(2, tabla.getRowCount());
            assertEquals("Urgente", tabla.getValueAt(0, 1));
            assertEquals("Activa", tabla.getValueAt(0, 4));
            assertFalse(tabla.getModel().isCellEditable(0, 0));
            tabla.setRowSelectionInterval(1, 1);
            assertFalse(buscar(
                    pantalla[0], "desactivar", JButton.class)
                    .isEnabled());
        });
        assertEquals(1, servicio.listados);
    }

    @Test
    void creaReglaDespuesDeValidarElFormato() throws Exception {
        ServicioFalso servicio = new ServicioFalso();
        PantallaPrueba[] pantalla = new PantallaPrueba[1];

        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaPrueba(
                        servicio, ADMINISTRADOR));
        SwingUtilities.invokeAndWait(() -> {
            buscar(pantalla[0], "nombre", JTextField.class)
                    .setText(" Urgente ");
            buscar(pantalla[0], "descripcion", JTextField.class)
                    .setText(" Prioridad alta ");
            buscar(pantalla[0], "orden", JTextField.class)
                    .setText("1");
            buscar(pantalla[0], "guardar", JButton.class).doClick();
        });

        assertEquals(1, servicio.creaciones);
        assertEquals(1, servicio.reglas.size());
        assertEquals("Urgente", servicio.reglas.get(0).getNombre());
        assertEquals("Prioridad alta",
                servicio.reglas.get(0).getDescripcion());
        assertEquals("Regla creada correctamente.",
                texto(pantalla[0], "mensaje"));
    }

    @Test
    void noEnviaAlServicioDatosConFormatoInvalido()
            throws Exception {
        ServicioFalso servicio = new ServicioFalso();
        PantallaPrueba[] pantalla = new PantallaPrueba[1];

        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaPrueba(
                        servicio, ADMINISTRADOR));
        SwingUtilities.invokeAndWait(() -> {
            buscar(pantalla[0], "nombre", JTextField.class)
                    .setText("Urgente");
            buscar(pantalla[0], "orden", JTextField.class)
                    .setText("uno");
            buscar(pantalla[0], "guardar", JButton.class).doClick();
        });

        assertEquals(0, servicio.creaciones);
        assertTrue(texto(pantalla[0], "mensaje").contains("entero"));
    }

    @Test
    void modificaLaReglaSeleccionada() throws Exception {
        ServicioFalso servicio = new ServicioFalso();
        servicio.agregar(regla(1, "Urgente", 1, true));
        PantallaPrueba[] pantalla = new PantallaPrueba[1];

        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaPrueba(
                        servicio, ADMINISTRADOR));
        SwingUtilities.invokeAndWait(() -> {
            buscar(pantalla[0], "tabla", JTable.class)
                    .setRowSelectionInterval(0, 0);
            buscar(pantalla[0], "nombre", JTextField.class)
                    .setText("Prioritaria");
            buscar(pantalla[0], "orden", JTextField.class)
                    .setText("2");
            buscar(pantalla[0], "guardar", JButton.class).doClick();
        });

        assertEquals(1, servicio.modificaciones);
        assertEquals("Prioritaria", servicio.reglas.get(0).getNombre());
        assertEquals("Regla modificada correctamente.",
                texto(pantalla[0], "mensaje"));
    }

    @Test
    void desactivaReglaActivaSiSeConfirma() throws Exception {
        ServicioFalso servicio = new ServicioFalso();
        servicio.agregar(regla(1, "Urgente", 1, true));
        PantallaPrueba[] pantalla = new PantallaPrueba[1];

        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaPrueba(
                        servicio, ADMINISTRADOR));
        SwingUtilities.invokeAndWait(() -> {
            buscar(pantalla[0], "tabla", JTable.class)
                    .setRowSelectionInterval(0, 0);
            buscar(pantalla[0], "desactivar", JButton.class)
                    .doClick();
        });

        assertEquals(1, servicio.desactivaciones);
        assertFalse(servicio.reglas.get(0).isActivo());
        assertEquals("Inactiva",
                buscar(pantalla[0], "tabla", JTable.class)
                        .getValueAt(0, 4));
        assertEquals("Regla desactivada correctamente.",
                texto(pantalla[0], "mensaje"));
    }

    @Test
    void noDesactivaSiSeCancelaLaConfirmacion() throws Exception {
        ServicioFalso servicio = new ServicioFalso();
        servicio.agregar(regla(1, "Urgente", 1, true));
        PantallaPrueba[] pantalla = new PantallaPrueba[1];

        SwingUtilities.invokeAndWait(() -> {
            pantalla[0] = new PantallaPrueba(servicio, ADMINISTRADOR);
            pantalla[0].confirmar = false;
        });
        SwingUtilities.invokeAndWait(() -> {
            buscar(pantalla[0], "tabla", JTable.class)
                    .setRowSelectionInterval(0, 0);
            buscar(pantalla[0], "desactivar", JButton.class)
                    .doClick();
        });

        assertEquals(0, servicio.desactivaciones);
        assertTrue(servicio.reglas.get(0).isActivo());
    }

    @Test
    void ocultaLaConfiguracionAUsuariosSinRolAdministrador()
            throws Exception {
        ServicioFalso servicio = new ServicioFalso();
        PantallaPrueba[] pantalla = new PantallaPrueba[1];

        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaPrueba(
                        servicio, new Usuario("medico", Rol.MEDICO)));

        assertNull(buscar(pantalla[0], "guardar", JButton.class));
        assertNull(buscar(pantalla[0], "tabla", JTable.class));
        assertEquals(0, servicio.listados);
        assertFalse(PantallaReglasPrioridad.disponiblePara(
                new Usuario("medico", Rol.MEDICO)));
        assertTrue(texto(pantalla[0], "mensaje")
                .contains("Administrador"));
    }

    @Test
    void muestraErroresAlCargarElListado() throws Exception {
        ServicioFalso servicio = new ServicioFalso();
        servicio.fallarListado = true;
        PantallaPrueba[] pantalla = new PantallaPrueba[1];

        SwingUtilities.invokeAndWait(() ->
                pantalla[0] = new PantallaPrueba(
                        servicio, ADMINISTRADOR));

        assertTrue(texto(pantalla[0], "mensaje")
                .contains("Error simulado"));
    }

    private static String texto(
            PantallaReglasPrioridad pantalla,
            String nombre) {
        return buscar(pantalla, nombre, JLabel.class).getText();
    }
}
