package tipoprueba;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PantallaTiposPruebaTest {

    private ServicioFalso servicio;

    @BeforeEach
    void setUp() {
        servicio = new ServicioFalso();

        servicio.datos.add(
                new TipoPrueba(
                        1L,
                        "Radiografía",
                        "Radiografía convencional",
                        15,
                        false,
                        "",
                        true));

        servicio.datos.add(
                new TipoPrueba(
                        2L,
                        "Resonancia",
                        "Resonancia magnética",
                        45,
                        true,
                        "Retirar objetos metálicos",
                        false));
    }

    @Test
    @DisplayName(
            "La pantalla carga los tipos al construirse")
    void constructorCargaListado()
            throws Exception {

        PantallaTiposPrueba pantalla =
                new PantallaTiposPrueba(
                        servicio,
                        Rol.ADMINISTRADOR);

        JTable tabla =
                campo(
                        pantalla,
                        "tabla",
                        JTable.class);

        assertEquals(
                2,
                tabla.getRowCount());

        assertEquals(
                "Radiografía",
                tabla.getValueAt(0, 0));

        assertEquals(
                "Activo",
                tabla.getValueAt(0, 2));

        assertEquals(
                "Inactivo",
                tabla.getValueAt(1, 2));
    }

    @Test
    @DisplayName(
            "Los botones de gestión son visibles para el administrador")
    void botonesVisiblesParaAdministrador()
            throws Exception {

        PantallaTiposPrueba pantalla =
                new PantallaTiposPrueba(
                        servicio,
                        Rol.ADMINISTRADOR);

        JButton nuevo =
                campo(
                        pantalla,
                        "btnNuevo",
                        JButton.class);

        JButton modificar =
                campo(
                        pantalla,
                        "btnModificar",
                        JButton.class);

        JButton desactivar =
                campo(
                        pantalla,
                        "btnDesactivar",
                        JButton.class);

        assertTrue(nuevo.isVisible());
        assertTrue(modificar.isVisible());
        assertTrue(desactivar.isVisible());
    }

    @Test
    @DisplayName(
            "Los botones de gestión no son visibles para otros roles")
    void botonesOcultosParaNoAdministrador()
            throws Exception {

        PantallaTiposPrueba pantalla =
                new PantallaTiposPrueba(
                        servicio,
                        Rol.MEDICO);

        JButton nuevo =
                campo(
                        pantalla,
                        "btnNuevo",
                        JButton.class);

        JButton modificar =
                campo(
                        pantalla,
                        "btnModificar",
                        JButton.class);

        JButton desactivar =
                campo(
                        pantalla,
                        "btnDesactivar",
                        JButton.class);

        assertFalse(nuevo.isVisible());
        assertFalse(modificar.isVisible());
        assertFalse(desactivar.isVisible());
    }

    @Test
    @DisplayName(
            "Sin selección no se puede modificar ni desactivar")
    void botonesDeshabilitadosSinSeleccion()
            throws Exception {

        PantallaTiposPrueba pantalla =
                new PantallaTiposPrueba(
                        servicio,
                        Rol.ADMINISTRADOR);

        JButton modificar =
                campo(
                        pantalla,
                        "btnModificar",
                        JButton.class);

        JButton desactivar =
                campo(
                        pantalla,
                        "btnDesactivar",
                        JButton.class);

        assertFalse(modificar.isEnabled());
        assertFalse(desactivar.isEnabled());
    }

    @Test
    @DisplayName(
            "Seleccionar un tipo activo permite modificarlo y desactivarlo")
    void seleccionarActivoHabilitaBotones()
            throws Exception {

        PantallaTiposPrueba pantalla =
                new PantallaTiposPrueba(
                        servicio,
                        Rol.ADMINISTRADOR);

        JTable tabla =
                campo(
                        pantalla,
                        "tabla",
                        JTable.class);

        JButton modificar =
                campo(
                        pantalla,
                        "btnModificar",
                        JButton.class);

        JButton desactivar =
                campo(
                        pantalla,
                        "btnDesactivar",
                        JButton.class);

        tabla.setRowSelectionInterval(0, 0);

        assertTrue(modificar.isEnabled());
        assertTrue(desactivar.isEnabled());
    }

    @Test
    @DisplayName(
            "Seleccionar un tipo inactivo permite modificar pero no desactivar")
    void seleccionarInactivoNoPermiteDesactivar()
            throws Exception {

        PantallaTiposPrueba pantalla =
                new PantallaTiposPrueba(
                        servicio,
                        Rol.ADMINISTRADOR);

        JTable tabla =
                campo(
                        pantalla,
                        "tabla",
                        JTable.class);

        JButton modificar =
                campo(
                        pantalla,
                        "btnModificar",
                        JButton.class);

        JButton desactivar =
                campo(
                        pantalla,
                        "btnDesactivar",
                        JButton.class);

        tabla.setRowSelectionInterval(1, 1);

        assertTrue(modificar.isEnabled());
        assertFalse(desactivar.isEnabled());
    }

    @Test
    @DisplayName(
            "Escribir en el buscador filtra automáticamente el listado")
    void buscadorFiltraListado()
            throws Exception {

        PantallaTiposPrueba pantalla =
                new PantallaTiposPrueba(
                        servicio,
                        Rol.ADMINISTRADOR);

        JTextField buscar =
                campo(
                        pantalla,
                        "txtBuscar",
                        JTextField.class);

        JTable tabla =
                campo(
                        pantalla,
                        "tabla",
                        JTable.class);

        buscar.setText("resonancia");

        assertEquals(
                1,
                tabla.getRowCount());

        assertEquals(
                "Resonancia",
                tabla.getValueAt(0, 0));
    }

    @Test
    @DisplayName(
            "El filtro de estado permite mostrar solo activos")
    void filtroSoloActivos()
            throws Exception {

        PantallaTiposPrueba pantalla =
                new PantallaTiposPrueba(
                        servicio,
                        Rol.ADMINISTRADOR);

        JComboBox<?> estado =
                campo(
                        pantalla,
                        "cmbEstado",
                        JComboBox.class);

        JTable tabla =
                campo(
                        pantalla,
                        "tabla",
                        JTable.class);

        estado.setSelectedItem(
                FiltroTipoPrueba.Estado.ACTIVOS);

        assertEquals(
                1,
                tabla.getRowCount());

        assertEquals(
                "Radiografía",
                tabla.getValueAt(0, 0));
    }

    @Test
    @DisplayName(
            "El filtro de estado permite mostrar solo inactivos")
    void filtroSoloInactivos()
            throws Exception {

        PantallaTiposPrueba pantalla =
                new PantallaTiposPrueba(
                        servicio,
                        Rol.ADMINISTRADOR);

        JComboBox<?> estado =
                campo(
                        pantalla,
                        "cmbEstado",
                        JComboBox.class);

        JTable tabla =
                campo(
                        pantalla,
                        "tabla",
                        JTable.class);

        estado.setSelectedItem(
                FiltroTipoPrueba.Estado.INACTIVOS);

        assertEquals(
                1,
                tabla.getRowCount());

        assertEquals(
                "Resonancia",
                tabla.getValueAt(0, 0));
    }

    @Test
    @DisplayName(
            "Un error del servicio se muestra en la pantalla")
    void errorDelServicioSeMuestra()
            throws Exception {

        servicio.fallarListado = true;

        PantallaTiposPrueba pantalla =
                new PantallaTiposPrueba(
                        servicio,
                        Rol.ADMINISTRADOR);

        JLabel mensaje =
                campo(
                        pantalla,
                        "lblMensaje",
                        JLabel.class);

        assertEquals(
                Mensajes.ERROR_GENERICO
                        + "Error simulado",
                mensaje.getText());
    }

    @Test
    @DisplayName(
            "Las celdas de la tabla no son editables")
    void tablaNoEditable()
            throws Exception {

        PantallaTiposPrueba pantalla =
                new PantallaTiposPrueba(
                        servicio,
                        Rol.ADMINISTRADOR);

        JTable tabla =
                campo(
                        pantalla,
                        "tabla",
                        JTable.class);

        DefaultTableModel modelo =
                (DefaultTableModel)
                        tabla.getModel();

        assertFalse(
                modelo.isCellEditable(0, 0));
    }

    private static <T> T campo(
            Object objeto,
            String nombre,
            Class<T> tipo)
            throws Exception {

        Field field =
                objeto.getClass()
                        .getDeclaredField(nombre);

        field.setAccessible(true);

        return tipo.cast(
                field.get(objeto));
    }

    private static class ServicioFalso
            implements TiposPruebaService {

        private final List<TipoPrueba> datos =
                new ArrayList<>();

        private boolean fallarListado;

        @Override
        public TipoPrueba crear(
                DatosTipoPrueba datos)
                throws ServicioException {

            return null;
        }

        @Override
        public TipoPrueba consultar(long id)
                throws ServicioException {

            return null;
        }

        @Override
        public List<TipoPrueba> listar(
                FiltroTipoPrueba filtro)
                throws ServicioException {

            if (fallarListado) {
                throw new ServicioException(
                        "Error simulado");
            }

            return datos.stream()
                    .filter(t ->
                            t.getNombre()
                                    .toLowerCase()
                                    .contains(
                                            filtro.getTexto()
                                                    .toLowerCase()))
                    .filter(t ->
                            filtro.getEstado()
                                    == FiltroTipoPrueba.Estado.TODOS
                            || filtro.getEstado()
                                    == FiltroTipoPrueba.Estado.ACTIVOS
                                    && t.isActivo()
                            || filtro.getEstado()
                                    == FiltroTipoPrueba.Estado.INACTIVOS
                                    && !t.isActivo())
                    .toList();
        }

        @Override
        public TipoPrueba modificar(
                long id,
                DatosTipoPrueba datos)
                throws ServicioException {

            return null;
        }

        @Override
        public void desactivar(long id)
                throws ServicioException {
        }
    }
}