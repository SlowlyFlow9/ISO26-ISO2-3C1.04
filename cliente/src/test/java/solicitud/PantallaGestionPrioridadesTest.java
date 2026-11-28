package solicitud;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static soporte.Dialogos.campo;
import static soporte.Dialogos.consultar;
import static soporte.Dialogos.enEdt;
import static soporte.Dialogos.esperar;
import static soporte.Dialogos.primero;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import paciente.Rol;
import paciente.Usuario;
import soporte.Dialogos;

/** CU28 - Cliente: uso de la pantalla de gestión de prioridades con el servicio simulado. */
class PantallaGestionPrioridadesTest {

    private static final Usuario ADMINISTRATIVO = new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO);
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 11, 20, 9, 0);

    private Dialogos dialogos;
    private PrioridadesServiceSimulado servicio;
    private PantallaGestionPrioridades pantalla;

    @BeforeEach
    void setUp() {
        dialogos = Dialogos.instalar();
        servicio = PrioridadesServiceSimulado.conDatosDeEjemplo();
    }

    @AfterEach
    void tearDown() {
        dialogos.close();
    }

    // ---- Utilidades ----

    private void abrir(PrioridadesService servicioPantalla) {
        enEdt(() -> {
            pantalla = new PantallaGestionPrioridades(servicioPantalla, ADMINISTRATIVO);
            pantalla.setVisible(true);
        });
        esperar(() -> boton("buscar").isEnabled() && !estado().endsWith("..."));
    }

    private void abrir() {
        abrir(servicio);
    }

    private JTable tabla() { return campo(pantalla, "tabla", JTable.class); }
    private int filas() { return tabla().getRowCount(); }
    private String estado() { return campo(pantalla, "estado", JLabel.class).getText(); }
    private JButton boton(String nombre) { return campo(pantalla, nombre, JButton.class); }

    private void seleccionar(long solicitudId) {
        enEdt(() -> {
            int fila = campo(pantalla, "modelo", ModeloTablaPrioridades.class).filaDe(solicitudId);
            tabla().setRowSelectionInterval(fila, fila);
        });
    }

    private void pulsarYEsperar(String nombre) {
        enEdt(() -> boton(nombre).doClick());
        esperar(() -> boton("buscar").isEnabled() && !estado().endsWith("..."));
    }

    /** Rellena el formulario de cambio (prioridad, regla por posición y motivo) y pulsa Aceptar. */
    @SuppressWarnings("unchecked")
    private static Consumer<JDialog> rellenarCambio(Prioridad prioridad, int regla, String motivo) {
        return dialogo -> {
            JOptionPane panel = primero(dialogo, JOptionPane.class);
            JPanel formulario = (JPanel) panel.getMessage();
            List<JComboBox> combos = Dialogos.buscar(formulario, JComboBox.class);
            combos.get(0).setSelectedItem(prioridad);
            combos.get(1).setSelectedIndex(regla);
            primero(formulario, JTextArea.class).setText(motivo);
            panel.setValue(JOptionPane.OK_OPTION);
        };
    }

    // ---- Listado ----

    @Test
    @DisplayName("Al abrir se cargan las solicitudes pendientes ordenadas por prioridad")
    void alAbrirCargaLasPendientes() {
        // Ejecutar
        abrir();
        // Comprobar
        assertAll(
                () -> assertEquals(4, consultar(this::filas)),
                () -> assertEquals("4 solicitud(es) pendiente(s)", consultar(this::estado)),
                () -> assertEquals(Prioridad.URGENTE, consultar(() -> tabla().getValueAt(0, 3))),
                () -> assertEquals("ID 1", consultar(() -> tabla().getValueAt(0, 2))),
                () -> assertFalse(consultar(() -> boton("cambiar").isEnabled())),
                () -> assertFalse(consultar(() -> boton("historial").isEnabled())));
    }

    @Test
    @DisplayName("El filtro por prioridad reduce el listado y se puede volver a ver todas")
    void filtroPorPrioridad() {
        // Preparar
        abrir();
        @SuppressWarnings("unchecked")
        JComboBox<Prioridad> filtro = campo(pantalla, "filtroPrioridad", JComboBox.class);
        // Ejecutar y comprobar
        enEdt(() -> filtro.setSelectedItem(Prioridad.ORDINARIA));
        pulsarYEsperar("buscar");
        assertEquals(2, consultar(this::filas));
        enEdt(() -> filtro.setSelectedItem(null));
        pulsarYEsperar("buscar");
        assertEquals(4, consultar(this::filas));
    }

    @Test
    @DisplayName("Si no hay solicitudes pendientes se indica en la pantalla")
    void sinSolicitudes() {
        // Ejecutar
        abrir(new PrioridadesServiceSimulado());
        // Comprobar
        assertEquals("No hay solicitudes pendientes con esa prioridad", consultar(this::estado));
    }

    @Test
    @DisplayName("Al seleccionar una solicitud se activan los botones")
    void seleccionarActivaBotones() {
        // Preparar
        abrir();
        // Ejecutar
        seleccionar(3);
        // Comprobar
        assertAll(
                () -> assertTrue(consultar(() -> boton("cambiar").isEnabled())),
                () -> assertTrue(consultar(() -> boton("historial").isEnabled())));
    }

    // ---- Cambio de prioridad ----

    @Test
    @DisplayName("Cambiar la prioridad la guarda, confirma, reordena la lista y mantiene la selección")
    void cambiarPrioridad() {
        // Preparar
        abrir();
        seleccionar(3);
        dialogos.responder(rellenarCambio(Prioridad.URGENTE, 2, "Más de 60 días esperando"));
        // Ejecutar
        pulsarYEsperar("cambiar");
        // Comprobar
        esperar(() -> dialogos.algunoContiene("cambiada de ORDINARIA a URGENTE"));
        esperar(() -> filas() == 4 && tabla().getSelectedRow() >= 0);
        assertAll(
                () -> assertEquals(Prioridad.URGENTE,
                        servicio.consultarPendientes(ADMINISTRATIVO, Prioridad.URGENTE).get(0).prioridad()),
                () -> assertEquals(3L, consultar(() -> tabla().getValueAt(tabla().getSelectedRow(), 0))),
                () -> assertEquals(1, servicio.consultarHistorial(ADMINISTRATIVO, 3).size()),
                () -> assertEquals(3, servicio.consultarHistorial(ADMINISTRATIVO, 3).get(0).reglaPrioridadId()));
    }

    @Test
    @DisplayName("Un cambio sin motivo o con la misma prioridad muestra el error y se puede cancelar")
    void cambioNoValidoYCancelar() {
        // Preparar
        abrir();
        seleccionar(3);
        dialogos.responder(rellenarCambio(Prioridad.ORDINARIA, 0, " "))
                .responder(Dialogos::aceptar)
                .responder(Dialogos::cancelar);
        // Ejecutar
        enEdt(() -> boton("cambiar").doClick());
        // Comprobar
        esperar(() -> dialogos.textos().size() == 3);
        assertAll(
                () -> assertTrue(dialogos.textos().get(1).contains("La nueva prioridad debe ser distinta de la actual")),
                () -> assertTrue(dialogos.textos().get(1).contains("El motivo del cambio es obligatorio")),
                () -> assertTrue(servicio.consultarHistorial(ADMINISTRATIVO, 3).isEmpty()));
    }

    @Test
    @DisplayName("Si la solicitud ya no está pendiente se muestra el error del servicio")
    void solicitudYaCitada() {
        // Preparar: la solicitud deja de estar pendiente después de cargar la pantalla
        PrioridadesServiceSimulado conCambio = new PrioridadesServiceSimulado()
                .conRegla(new ReglaPrioridadAplicable(1, "Oncológico", "", 1), true)
                .conSolicitud(new SolicitudPrioridad(7, 70, 1, Prioridad.ORDINARIA, FECHA), true);
        abrir(conCambio);
        seleccionar(7);
        conCambio.conSolicitud(new SolicitudPrioridad(7, 70, 1, Prioridad.ORDINARIA, FECHA), false);
        dialogos.responder(rellenarCambio(Prioridad.URGENTE, 0, "Motivo"));
        // Ejecutar
        pulsarYEsperar("cambiar");
        // Comprobar
        esperar(() -> dialogos.algunoContiene("ya no está pendiente de citación"));
        assertEquals("No se ha cambiado la prioridad", consultar(this::estado));
    }

    @Test
    @DisplayName("Sin reglas activas no se puede cambiar la prioridad")
    void sinReglasActivas() {
        // Preparar
        abrir(new PrioridadesServiceSimulado()
                .conSolicitud(new SolicitudPrioridad(7, 70, 1, Prioridad.ORDINARIA, FECHA), true));
        seleccionar(7);
        // Ejecutar
        enEdt(() -> boton("cambiar").doClick());
        // Comprobar
        assertTrue(dialogos.algunoContiene("No hay reglas de prioridad activas"));
    }

    // ---- Historial ----

    @Test
    @DisplayName("El historial muestra los cambios con el nombre de la regla")
    void verHistorial() {
        // Preparar
        servicio.cambiarPrioridad(ADMINISTRATIVO, 3,
                new DatosCambioPrioridad(Prioridad.PREFERENTE, 3, "Espera larga"));
        abrir();
        seleccionar(3);
        String[] texto = new String[1];
        dialogos.responder(dialogo -> {
            texto[0] = primero(dialogo, JTextArea.class).getText();
            Dialogos.aceptar(dialogo);
        });
        // Ejecutar
        pulsarYEsperar("historial");
        // Comprobar
        esperar(() -> texto[0] != null);
        assertAll(
                () -> assertTrue(texto[0].contains("ORDINARIA → PREFERENTE")),
                () -> assertTrue(texto[0].contains("Regla: Espera prolongada")),
                () -> assertTrue(texto[0].contains("Motivo: Espera larga")),
                () -> assertEquals("1 cambio(s) de prioridad", consultar(this::estado)));
    }

    @Test
    @DisplayName("El texto del historial indica cuando no hay cambios y usa el id si la regla no se conoce")
    void textoHistorial() {
        CambioPrioridad cambio = new CambioPrioridad(1, 5, Prioridad.ORDINARIA, Prioridad.URGENTE, 9,
                "M", "admin", FECHA);
        assertAll(
                () -> assertEquals("La solicitud 5 no tiene cambios de prioridad.",
                        ModeloTablaPrioridades.textoHistorial(5, List.of(), Map.of())),
                () -> assertTrue(ModeloTablaPrioridades.textoHistorial(5, List.of(cambio), Map.of())
                        .contains("Regla: ID 9")),
                () -> assertTrue(ModeloTablaPrioridades.textoHistorial(5, List.of(cambio), Map.of())
                        .contains("20/11/2026 09:00 · admin")));
    }

    // ---- Errores y permisos ----

    @Test
    @DisplayName("Si fallan las reglas se avisa y se cargan igualmente las solicitudes")
    void fallanLasReglas() {
        // Preparar
        PrioridadesServiceSimulado sinReglas = new PrioridadesServiceSimulado() {
            @Override
            public List<ReglaPrioridadAplicable> consultarReglasActivas(Usuario solicitante) {
                throw new IllegalStateException("Servidor caído");
            }
        }.conSolicitud(new SolicitudPrioridad(7, 70, 1, Prioridad.ORDINARIA, FECHA), true);
        // Ejecutar
        abrir(sinReglas);
        // Comprobar: primero se avisa del error y después se cargan las solicitudes
        esperar(() -> dialogos.algunoContiene("Servidor caído"));
        esperar(() -> filas() == 1);
        assertEquals("1 solicitud(es) pendiente(s)", consultar(this::estado));
    }

    @ParameterizedTest
    @EnumSource(value = Rol.class, names = "PERSONAL_ADMINISTRATIVO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Un usuario que no es personal administrativo no puede abrir la pantalla")
    void sinPermisoNoAbre(Rol rol) {
        assertAll(
                () -> assertFalse(PantallaGestionPrioridades.disponiblePara(new Usuario("x", rol))),
                () -> assertThrows(IllegalArgumentException.class, () -> enEdt(() ->
                        new PantallaGestionPrioridades(servicio, new Usuario("x", rol)))));
    }

    @Test
    @DisplayName("Un usuario nulo no puede abrir la pantalla")
    void usuarioNulo() {
        assertFalse(PantallaGestionPrioridades.disponiblePara(null));
    }

    @Test
    @DisplayName("El arranque sin URL abre la pantalla con el servicio simulado")
    void mainConServicioSimulado() {
        // Ejecutar
        PantallaGestionPrioridades.main(new String[0]);
        // Comprobar
        esperar(() -> java.util.Arrays.stream(java.awt.Window.getWindows())
                .anyMatch(v -> v instanceof PantallaGestionPrioridades && v.isShowing()));
    }

    @Test
    @DisplayName("El arranque con un rol sin permiso muestra un aviso")
    void mainSinPermiso() {
        // Ejecutar
        PantallaGestionPrioridades.main(new String[] {"MEDICO"});
        // Comprobar
        assertTrue(dialogos.algunoContiene("Solo el personal administrativo puede gestionar prioridades"));
    }
}
