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

import java.util.List;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;
import soporte.Dialogos;
import tipoprueba.TipoPrueba;

/** CU13 - Cliente: uso de la pantalla de gestión de solicitudes con el servicio simulado. */
class PantallaGestionSolicitudesUiTest {

    private static final Usuario MEDICO = new Usuario("medico", Rol.MEDICO);

    private Dialogos dialogos;
    private SolicitudesServiceSimulado servicio;
    private PantallaGestionSolicitudes pantalla;

    @BeforeEach
    void setUp() {
        dialogos = Dialogos.instalar();
        servicio = SolicitudesServiceSimulado.conDatosDeEjemplo();
    }

    @AfterEach
    void tearDown() {
        dialogos.close();
    }

    // ---- Utilidades ----

    private void abrir(SolicitudesService servicioPantalla) {
        enEdt(() -> {
            pantalla = new PantallaGestionSolicitudes(servicioPantalla, MEDICO);
            pantalla.setVisible(true);
        });
        esperar(() -> estado().startsWith(filas() + " solicitud") || estado().startsWith("No hay"));
    }

    private void abrir() {
        abrir(servicio);
    }

    private JTable tabla() { return campo(pantalla, "tabla", JTable.class); }
    private int filas() { return tabla().getRowCount(); }
    private String estado() { return campo(pantalla, "estado", JLabel.class).getText(); }
    private String detalle() { return campo(pantalla, "detalle", JTextArea.class).getText(); }
    private JButton boton(String nombre) { return campo(pantalla, nombre, JButton.class); }

    @SuppressWarnings("unchecked")
    private <E> JComboBox<E> combo(String nombre) { return campo(pantalla, nombre, JComboBox.class); }

    private void seleccionar(long id) {
        enEdt(() -> {
            ModeloTablaSolicitudes modelo = campo(pantalla, "modelo", ModeloTablaSolicitudes.class);
            int fila = modelo.filaDe(id);
            tabla().setRowSelectionInterval(fila, fila);
        });
    }

    private void buscarCon(String paciente, EstadoSolicitud estado, Prioridad prioridad) {
        enEdt(() -> {
            campo(pantalla, "filtroPaciente", JTextField.class).setText(paciente);
            combo("filtroEstado").setSelectedItem(estado);
            combo("filtroPrioridad").setSelectedItem(prioridad);
            boton("buscar").doClick();
        });
        esperar(() -> boton("buscar").isEnabled() && !estado().startsWith("Buscando"));
    }

    private EstadoSolicitud estadoEnServicio(long id) {
        return servicio.consultarSolicitud(MEDICO, id).getEstado();
    }

    /** Rellena el formulario de modificación y pulsa Aceptar. */
    private static Consumer<JDialog> rellenarModificacion(String indicacion, Prioridad prioridad, String intervalo) {
        return dialogo -> {
            JOptionPane panel = primero(dialogo, JOptionPane.class);
            JPanel formulario = (JPanel) panel.getMessage();
            Dialogos.buscar(formulario, JTextArea.class).get(0).setText(indicacion);
            Dialogos.buscar(formulario, JComboBox.class).get(0).setSelectedItem(prioridad);
            List<JTextField> campos = Dialogos.buscar(formulario, JTextField.class);
            if (intervalo != null && !campos.isEmpty()) campos.get(0).setText(intervalo);
            panel.setValue(JOptionPane.OK_OPTION);
        };
    }

    /** Escribe el motivo de la anulación y pulsa Aceptar. */
    private static Consumer<JDialog> escribirMotivo(String motivo) {
        return dialogo -> {
            JOptionPane panel = primero(dialogo, JOptionPane.class);
            primero((JPanel) panel.getMessage(), JTextArea.class).setText(motivo);
            panel.setValue(JOptionPane.OK_OPTION);
        };
    }

    // ---- Apertura y listado ----

    @Test
    @DisplayName("Al abrir se cargan todas las solicitudes con el nombre de su tipo de prueba")
    void alAbrirCargaLasSolicitudes() {
        // Ejecutar
        abrir();
        // Comprobar
        assertAll(
                () -> assertEquals(5, consultar(this::filas)),
                () -> assertEquals("5 solicitud(es) encontrada(s)", consultar(this::estado)),
                () -> assertEquals("Radiografía", consultar(() -> tabla().getValueAt(0, 2))),
                () -> assertTrue(consultar(this::detalle).startsWith("Selecciona una solicitud")),
                () -> assertFalse(consultar(() -> boton("modificar").isEnabled())));
    }

    @Test
    @DisplayName("Los filtros por paciente, estado y prioridad reducen el listado y se pueden limpiar")
    void filtrosYLimpiar() {
        // Preparar
        abrir();
        // Ejecutar y comprobar
        buscarCon("101", null, null);
        assertEquals(2, consultar(this::filas));
        buscarCon("", EstadoSolicitud.ANULADA, null);
        assertEquals(1, consultar(this::filas));
        buscarCon("", null, Prioridad.PREFERENTE);
        assertEquals(2, consultar(this::filas));
        buscarCon("101", EstadoSolicitud.CITADA, null);
        assertEquals("No hay solicitudes con esos filtros", consultar(this::estado));

        enEdt(() -> boton("limpiar").doClick());
        esperar(() -> filas() == 5);
        assertEquals("", consultar(() -> campo(pantalla, "filtroPaciente", JTextField.class).getText()));
    }

    @Test
    @DisplayName("Un ID de paciente no válido en el filtro muestra un error y no busca")
    void filtroPacienteNoValido() {
        // Preparar
        abrir();
        // Ejecutar
        enEdt(() -> {
            campo(pantalla, "filtroPaciente", JTextField.class).setText("abc");
            boton("buscar").doClick();
        });
        // Comprobar
        esperar(() -> dialogos.algunoContiene("El ID del paciente debe ser un número positivo"));
        assertEquals(5, consultar(this::filas));
    }

    @Test
    @DisplayName("Seleccionar una solicitud muestra su detalle y activa los botones solo si está pendiente")
    void seleccionarMuestraDetalleYBotones() {
        // Preparar
        abrir();
        // Ejecutar y comprobar: pendiente de citación
        seleccionar(3);
        assertAll(
                () -> assertTrue(consultar(this::detalle).contains("Seguimiento: cada 3 MESES")),
                () -> assertTrue(consultar(() -> boton("modificar").isEnabled())),
                () -> assertTrue(consultar(() -> boton("anular").isEnabled())));
        // Ejecutar y comprobar: anulada
        seleccionar(5);
        assertAll(
                () -> assertTrue(consultar(this::detalle).contains("Motivo de la anulación: Solicitud duplicada")),
                () -> assertFalse(consultar(() -> boton("modificar").isEnabled())),
                () -> assertFalse(consultar(() -> boton("anular").isEnabled())));
    }

    // ---- Modificación ----

    @Test
    @DisplayName("Modificar una solicitud pendiente guarda los cambios y mantiene la selección")
    void modificarSolicitudPendiente() {
        // Preparar
        abrir();
        seleccionar(1);
        dialogos.responder(rellenarModificacion("Tos con fiebre", Prioridad.PREFERENTE, null));
        // Ejecutar
        enEdt(() -> boton("modificar").doClick());
        // Comprobar
        esperar(() -> dialogos.algunoContiene("Solicitud 1 modificada correctamente."));
        esperar(() -> detalle().contains("Tos con fiebre"));
        SolicitudPrueba guardada = servicio.consultarSolicitud(MEDICO, 1);
        assertAll(
                () -> assertEquals("Tos con fiebre", guardada.getIndicacionClinica()),
                () -> assertEquals(Prioridad.PREFERENTE, guardada.getPrioridad()),
                () -> assertEquals(1L, consultar(() -> tabla().getValueAt(tabla().getSelectedRow(), 0))));
    }

    @Test
    @DisplayName("Modificar un seguimiento permite cambiar su periodicidad")
    void modificarSeguimiento() {
        // Preparar
        abrir();
        seleccionar(3);
        dialogos.responder(rellenarModificacion("Control de lesión", Prioridad.ORDINARIA, "6"));
        // Ejecutar
        enEdt(() -> boton("modificar").doClick());
        // Comprobar
        esperar(() -> dialogos.algunoContiene("Solicitud 3 modificada correctamente."));
        assertEquals(6, servicio.consultarSolicitud(MEDICO, 3).getIntervalo());
    }

    @Test
    @DisplayName("Si la modificación no es válida se muestra el error, se vuelve al formulario y se puede cancelar")
    void modificacionNoValidaYCancelar() {
        // Preparar
        abrir();
        seleccionar(1);
        dialogos.responder(rellenarModificacion(" ", Prioridad.URGENTE, null))
                .responder(Dialogos::aceptar)
                .responder(Dialogos::cancelar);
        // Ejecutar
        enEdt(() -> boton("modificar").doClick());
        // Comprobar
        esperar(() -> dialogos.textos().size() == 3);
        assertAll(
                () -> assertTrue(dialogos.textos().get(1).contains("La indicación clínica es obligatoria")),
                () -> assertEquals("Dolor torácico", servicio.consultarSolicitud(MEDICO, 1).getIndicacionClinica()));
    }

    @Test
    @DisplayName("Si la solicitud cambió de estado en el servidor se muestra el error de estado")
    void modificarSolicitudYaNoPendiente() {
        // Preparar: la solicitud se anula por otro lado después de cargar la pantalla
        abrir();
        seleccionar(1);
        servicio.anularSolicitud(MEDICO, 1, "Anulada desde otro puesto");
        dialogos.responder(rellenarModificacion("Otro texto", Prioridad.URGENTE, null));
        // Ejecutar
        enEdt(() -> boton("modificar").doClick());
        // Comprobar
        esperar(() -> dialogos.algunoContiene("no esta pendiente de citacion"));
        esperar(() -> estado().equals("No se ha modificado la solicitud"));
    }

    // ---- Anulación ----

    @Test
    @DisplayName("Anular con motivo y confirmación pasa la solicitud a ANULADA")
    void anularSolicitud() {
        // Preparar
        abrir();
        seleccionar(1);
        dialogos.responder(escribirMotivo("Ya no es necesaria")).responder(Dialogos::aceptar);
        // Ejecutar
        enEdt(() -> boton("anular").doClick());
        // Comprobar
        esperar(() -> dialogos.algunoContiene("Solicitud 1 anulada correctamente."));
        esperar(() -> detalle().contains("Motivo de la anulación: Ya no es necesaria"));
        assertAll(
                () -> assertEquals(EstadoSolicitud.ANULADA, estadoEnServicio(1)),
                () -> assertFalse(consultar(() -> boton("anular").isEnabled())));
    }

    @Test
    @DisplayName("Anular sin motivo muestra un error y se puede cancelar sin anular")
    void anularSinMotivo() {
        // Preparar
        abrir();
        seleccionar(1);
        dialogos.responder(escribirMotivo("  ")).responder(Dialogos::aceptar).responder(Dialogos::cancelar);
        // Ejecutar
        enEdt(() -> boton("anular").doClick());
        // Comprobar
        esperar(() -> dialogos.textos().size() == 3);
        assertAll(
                () -> assertTrue(dialogos.textos().get(1).contains("El motivo de la anulación es obligatorio")),
                () -> assertEquals(EstadoSolicitud.PENDIENTE_CITACION, estadoEnServicio(1)));
    }

    @Test
    @DisplayName("Si no se confirma la anulación la solicitud no cambia")
    void anularSinConfirmar() {
        // Preparar
        abrir();
        seleccionar(1);
        dialogos.responder(escribirMotivo("Motivo")).responder(Dialogos::no);
        // Ejecutar
        enEdt(() -> boton("anular").doClick());
        // Comprobar
        esperar(() -> dialogos.textos().size() == 2);
        assertAll(
                () -> assertTrue(dialogos.textos().get(1).contains("¿Seguro que quieres anular la solicitud 1?")),
                () -> assertEquals(EstadoSolicitud.PENDIENTE_CITACION, estadoEnServicio(1)));
    }

    // ---- Errores del servicio y permisos ----

    @Test
    @DisplayName("Si fallan los tipos de prueba se avisa y se cargan igualmente las solicitudes")
    void fallanLosTiposDePrueba() {
        // Preparar
        SolicitudesServiceSimulado sinTipos = new SolicitudesServiceSimulado(List.of(),
                servicio.consultarSolicitudes(MEDICO, FiltroSolicitudes.vacio())) {
            @Override
            public List<TipoPrueba> consultarTiposPrueba(Usuario medico) {
                throw new IllegalStateException("Servidor caído");
            }
        };
        // Ejecutar
        abrir(sinTipos);
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("Servidor caído")),
                () -> assertEquals(5, consultar(this::filas)),
                () -> assertEquals("ID 1", consultar(() -> tabla().getValueAt(0, 2))));
    }

    @Test
    @DisplayName("Un usuario sin rol de Médico no puede abrir la pantalla")
    void sinRolMedicoNoAbre() {
        assertThrows(IllegalArgumentException.class, () -> enEdt(() ->
                new PantallaGestionSolicitudes(servicio, new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO))));
    }

    @Test
    @DisplayName("El arranque sin URL abre la pantalla con el servicio simulado")
    void mainAbreConServicioSimulado() {
        // Ejecutar
        PantallaGestionSolicitudes.main(new String[0]);
        // Comprobar
        esperar(() -> java.util.Arrays.stream(java.awt.Window.getWindows())
                .anyMatch(v -> v instanceof PantallaGestionSolicitudes && v.isShowing()));
    }

    @Test
    @DisplayName("El arranque con un rol que no es Médico muestra un aviso de permiso")
    void mainSinPermisoAvisa() {
        // Ejecutar
        PantallaGestionSolicitudes.main(new String[] {"PACIENTE"});
        // Comprobar
        assertTrue(dialogos.algunoContiene("Solo el rol Medico puede gestionar solicitudes"));
    }
}
