package tipoprueba;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static soporte.Dialogos.campo;
import static soporte.Dialogos.consultar;
import static soporte.Dialogos.enEdt;
import static soporte.Dialogos.esperar;

import java.util.List;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import soporte.Dialogos;

/** CU33 - Cliente: uso de la pantalla de tipos de prueba y de su formulario con el servicio simulado. */
class PantallaTiposPruebaUiTest {

    private Dialogos dialogos;
    private TiposPruebaServicioSimulado servicio;
    private PantallaTiposPrueba pantalla;

    @BeforeEach
    void setUp() {
        dialogos = Dialogos.instalar();
        servicio = new TiposPruebaServicioSimulado();
    }

    @AfterEach
    void tearDown() {
        dialogos.close();
    }

    // ---- Utilidades ----

    private void abrir(TiposPruebaService servicioPantalla, Rol rol) {
        enEdt(() -> {
            pantalla = new PantallaTiposPrueba(servicioPantalla, rol);
            JFrame ventana = new JFrame("Tipos de prueba");
            ventana.setContentPane(pantalla);
            ventana.setSize(700, 400);
            ventana.setVisible(true);
        });
    }

    private void abrir() {
        abrir(servicio, Rol.ADMINISTRADOR);
    }

    private JTable tabla() { return campo(pantalla, "tabla", JTable.class); }
    private int filas() { return consultar(() -> tabla().getRowCount()); }
    private JButton boton(String nombre) { return campo(pantalla, nombre, JButton.class); }
    private String mensaje() { return consultar(() -> campo(pantalla, "lblMensaje", JLabel.class).getText()); }

    private void seleccionar(int fila) {
        enEdt(() -> tabla().setRowSelectionInterval(fila, fila));
    }

    private void pulsar(String nombre) {
        enEdt(() -> boton(nombre).doClick());
    }

    /** Rellena el formulario de tipo de prueba, pulsa Guardar y, si sigue abierto, lo cancela. */
    private static Consumer<JDialog> rellenar(String nombre, String duracion, boolean preparacion, String instrucciones) {
        return dialogo -> {
            FormularioTipoPruebaDialog formulario = (FormularioTipoPruebaDialog) dialogo;
            campo(formulario, "txtNombre", JTextField.class).setText(nombre);
            campo(formulario, "txtDuracion", JTextField.class).setText(duracion);
            JCheckBox preparar = campo(formulario, "chkPreparacion", JCheckBox.class);
            if (preparar.isSelected() != preparacion) preparar.doClick();
            campo(formulario, "txtInstrucciones", JTextArea.class).setText(instrucciones);
            Dialogos.boton(formulario, "Guardar").doClick();
            if (formulario.isShowing()) {
                ERRORES.add(campo(formulario, "lblError", JLabel.class).getText());
                Dialogos.boton(formulario, "Cancelar").doClick();
            }
        };
    }

    private static final List<String> ERRORES = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

    // ---- Listado y filtros ----

    @Test
    @DisplayName("El administrador ve los tipos de prueba y los botones de gestión")
    void administradorVeListadoYBotones() {
        // Ejecutar
        abrir();
        // Comprobar
        assertAll(
                () -> assertEquals(3, filas()),
                () -> assertEquals("Radiografía de tórax", consultar(() -> tabla().getValueAt(0, 0))),
                () -> assertEquals("Activo", consultar(() -> tabla().getValueAt(0, 2))),
                () -> assertTrue(consultar(() -> boton("btnNuevo").isVisible())),
                () -> assertFalse(consultar(() -> boton("btnModificar").isEnabled())));
    }

    @Test
    @DisplayName("Un médico no ve los botones de gestión")
    void medicoNoVeBotones() {
        // Ejecutar
        abrir(servicio, Rol.MEDICO);
        // Comprobar
        assertAll(
                () -> assertFalse(consultar(() -> boton("btnNuevo").isVisible())),
                () -> assertFalse(consultar(() -> boton("btnModificar").isVisible())),
                () -> assertFalse(consultar(() -> boton("btnDesactivar").isVisible())));
    }

    @Test
    @DisplayName("La búsqueda por nombre y el filtro de estado reducen el listado")
    void busquedaYFiltroDeEstado() {
        // Preparar
        abrir();
        // Ejecutar y comprobar
        enEdt(() -> campo(pantalla, "txtBuscar", JTextField.class).setText("reso"));
        assertEquals(1, filas());
        enEdt(() -> campo(pantalla, "txtBuscar", JTextField.class).setText(""));
        enEdt(() -> campo(pantalla, "cmbEstado", JComboBox.class).setSelectedItem(FiltroTipoPrueba.Estado.INACTIVOS));
        assertEquals(0, filas());
        enEdt(() -> campo(pantalla, "cmbEstado", JComboBox.class).setSelectedItem(FiltroTipoPrueba.Estado.ACTIVOS));
        assertEquals(3, filas());
    }

    // ---- Alta ----

    @Test
    @DisplayName("Dar de alta un tipo de prueba válido lo añade al listado")
    void altaValida() {
        // Preparar
        abrir();
        dialogos.responder(rellenar("Mamografía", "20", true, "No usar desodorante"));
        // Ejecutar
        pulsar("btnNuevo");
        // Comprobar
        assertAll(
                () -> assertEquals(4, filas()),
                () -> assertEquals(Mensajes.EXITO_CREAR, mensaje()),
                () -> assertTrue(dialogos.algunoContiene("Nuevo tipo de prueba")));
    }

    @Test
    @DisplayName("Un alta con datos no válidos muestra el error en el formulario y no se guarda")
    void altaNoValida() {
        // Preparar
        abrir();
        ERRORES.clear();
        dialogos.responder(rellenar("", "abc", false, ""));
        // Ejecutar
        pulsar("btnNuevo");
        // Comprobar
        assertAll(
                () -> assertEquals(3, filas()),
                () -> assertTrue(ERRORES.get(0).contains(Mensajes.NOMBRE_OBLIGATORIO)),
                () -> assertEquals(" ", mensaje()));
    }

    @Test
    @DisplayName("Un alta con un nombre repetido muestra el error del servicio y conserva el formulario")
    void altaNombreDuplicado() {
        // Preparar
        abrir();
        ERRORES.clear();
        dialogos.responder(rellenar("radiografía de tórax", "15", false, ""));
        // Ejecutar
        pulsar("btnNuevo");
        // Comprobar
        assertAll(
                () -> assertEquals(3, filas()),
                () -> assertTrue(ERRORES.get(0).contains("Ya existe un tipo de prueba")));
    }

    // ---- Modificación ----

    @Test
    @DisplayName("Modificar el tipo seleccionado muestra sus datos y guarda los cambios")
    void modificarTipo() {
        // Preparar
        abrir();
        seleccionar(1);
        dialogos.responder(dialogo -> {
            FormularioTipoPruebaDialog formulario = (FormularioTipoPruebaDialog) dialogo;
            assertEquals("Resonancia cerebral", campo(formulario, "txtNombre", JTextField.class).getText());
            assertTrue(campo(formulario, "txtInstrucciones", JTextArea.class).isEnabled());
            campo(formulario, "txtDuracion", JTextField.class).setText("50");
            Dialogos.boton(formulario, "Guardar").doClick();
        });
        // Ejecutar
        pulsar("btnModificar");
        // Comprobar
        assertAll(
                () -> assertEquals(50, consultar(() -> tabla().getValueAt(1, 1))),
                () -> assertEquals(Mensajes.EXITO_MODIFICAR, mensaje()),
                () -> assertTrue(dialogos.algunoContiene("Modificar tipo de prueba")));
    }

    @Test
    @DisplayName("Cancelar la modificación no cambia nada")
    void cancelarModificacion() {
        // Preparar
        abrir();
        seleccionar(0);
        dialogos.responder(dialogo -> Dialogos.boton(dialogo, "Cancelar").doClick());
        // Ejecutar
        pulsar("btnModificar");
        // Comprobar
        assertEquals(" ", mensaje());
    }

    // ---- Desactivación ----

    @Test
    @DisplayName("Desactivar avisa de las pruebas pendientes y, si se confirma, deja el tipo inactivo")
    void desactivarConPendientes() {
        // Preparar
        abrir();
        seleccionar(1);
        // Ejecutar
        pulsar("btnDesactivar");
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("hay 3 prueba(s) pendiente(s)")),
                () -> assertEquals("Inactivo", consultar(() -> tabla().getValueAt(1, 2))),
                () -> assertEquals(Mensajes.EXITO_DESACTIVAR, mensaje()));
    }

    @Test
    @DisplayName("Si no se confirma la desactivación el tipo sigue activo")
    void desactivarSinConfirmar() {
        // Preparar
        abrir();
        seleccionar(0);
        dialogos.responder(Dialogos::no);
        // Ejecutar
        pulsar("btnDesactivar");
        // Comprobar
        assertEquals("Activo", consultar(() -> tabla().getValueAt(0, 2)));
    }

    // ---- Errores del servicio ----

    @Test
    @DisplayName("Si el servicio falla al listar se muestra el error en la pantalla")
    void servicioFallaAlListar() {
        // Preparar
        TiposPruebaService conError = new TiposPruebaService() {
            @Override public TipoPrueba crear(DatosTipoPrueba d) throws ServicioException { throw error(); }
            @Override public TipoPrueba consultar(long id) throws ServicioException { throw error(); }
            @Override public List<TipoPrueba> listar(FiltroTipoPrueba f) throws ServicioException { throw error(); }
            @Override public TipoPrueba modificar(long id, DatosTipoPrueba d) throws ServicioException { throw error(); }
            @Override public void desactivar(long id) throws ServicioException { throw error(); }
            private ServicioException error() { return new ServicioException("Servidor no disponible"); }
        };
        // Ejecutar
        abrir(conError, Rol.ADMINISTRADOR);
        // Comprobar
        assertAll(
                () -> assertEquals(Mensajes.ERROR_GENERICO + "Servidor no disponible", mensaje()),
                () -> assertEquals(0, filas()));
    }

    @Test
    @DisplayName("El servicio simulado rechaza consultar o desactivar un tipo inexistente")
    void servicioSimuladoTipoInexistente() throws ServicioException {
        // Ejecutar y comprobar
        assertAll(
                () -> assertEquals("Ecografía abdominal", servicio.consultar(3).getNombre()),
                () -> assertEquals(0, servicio.contarPruebasPendientes(1)),
                () -> org.junit.jupiter.api.Assertions.assertThrows(ServicioException.class, () -> servicio.consultar(99)),
                () -> org.junit.jupiter.api.Assertions.assertThrows(ServicioException.class, () -> servicio.desactivar(99)));
    }

    @Test
    @DisplayName("El arranque de prueba abre la ventana de tipos de prueba")
    void appAbreLaVentana() {
        // Ejecutar
        AppTiposPrueba.main(new String[0]);
        // Comprobar
        esperar(() -> java.util.Arrays.stream(java.awt.Window.getWindows())
                .anyMatch(v -> v instanceof JFrame f && "Tipos de pruebas diagnósticas".equals(f.getTitle())
                        && f.isShowing()));
        enEdt(() -> java.util.Arrays.stream(java.awt.Window.getWindows())
                .filter(v -> v instanceof JFrame).forEach(v -> ((JFrame) v)
                        .setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE)));
    }
}
