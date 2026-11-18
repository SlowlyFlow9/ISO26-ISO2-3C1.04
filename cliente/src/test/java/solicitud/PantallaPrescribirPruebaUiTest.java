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

import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
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

/** CU11 y CU12 - Cliente: uso de la pantalla de prescripción con el servicio simulado. */
class PantallaPrescribirPruebaUiTest {

    private static final Usuario MEDICO = new Usuario("medico", Rol.MEDICO);

    private Dialogos dialogos;
    private SolicitudesServiceSimulado servicio;
    private PantallaPrescribirPrueba pantalla;

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
            pantalla = new PantallaPrescribirPrueba(servicioPantalla, MEDICO);
            pantalla.setVisible(true);
        });
        esperar(() -> boton("actualizar").isEnabled());
    }

    private void abrir() {
        abrir(servicio);
    }

    private JButton boton(String nombre) { return campo(pantalla, nombre, JButton.class); }
    private JTextField texto(String nombre) { return campo(pantalla, nombre, JTextField.class); }
    private JTextArea area(String nombre) { return campo(pantalla, nombre, JTextArea.class); }
    private String estado() { return campo(pantalla, "estado", JLabel.class).getText(); }

    private void rellenarObligatorios() {
        enEdt(() -> {
            texto("pacienteId").setText("101");
            texto("medicoId").setText("7");
            area("indicacion").setText("Dolor lumbar");
            area("justificacion").setText("Descartar hernia");
        });
    }

    private void registrar() {
        enEdt(() -> boton("guardar").doClick());
    }

    private int numeroSolicitudes() {
        return servicio.consultarSolicitudes(MEDICO, FiltroSolicitudes.vacio()).size();
    }

    // ---- Carga de tipos ----

    @Test
    @DisplayName("Al abrir se cargan los tipos de prueba activos y se puede registrar")
    void alAbrirCargaLosTipos() {
        // Ejecutar
        abrir();
        // Comprobar
        assertAll(
                () -> assertEquals(3, consultar(() -> campo(pantalla, "tipos", JComboBox.class).getItemCount())),
                () -> assertEquals("Tipos de prueba cargados correctamente", consultar(this::estado)),
                () -> assertTrue(consultar(() -> boton("guardar").isEnabled())),
                () -> assertFalse(consultar(() -> texto("intervalo").isEnabled())));
    }

    @Test
    @DisplayName("Sin tipos de prueba activos no se puede registrar")
    void sinTiposNoSePuedeRegistrar() {
        // Preparar
        SolicitudesServiceSimulado sinActivos = new SolicitudesServiceSimulado(
                List.of(new TipoPrueba(9, "Antigua", "", 10, false, "", false)), List.of());
        // Ejecutar
        abrir(sinActivos);
        // Comprobar
        assertAll(
                () -> assertEquals("No hay tipos de prueba disponibles", consultar(this::estado)),
                () -> assertFalse(consultar(() -> boton("guardar").isEnabled())));
    }

    @Test
    @DisplayName("Si falla la carga de tipos se muestra el error")
    void fallaLaCargaDeTipos() {
        // Preparar
        SolicitudesServiceSimulado caido = new SolicitudesServiceSimulado(List.of(), List.of()) {
            @Override
            public List<TipoPrueba> consultarTiposPrueba(Usuario medico) {
                throw new IllegalStateException("Servidor caído");
            }
        };
        // Ejecutar
        abrir(caido);
        // Comprobar
        esperar(() -> dialogos.algunoContiene("Servidor caído"));
        assertEquals("No se ha registrado la solicitud", consultar(this::estado));
    }

    // ---- Registro ----

    @Test
    @DisplayName("Una prescripción válida se registra, se confirma y se limpia el formulario")
    void registrarPrescripcion() {
        // Preparar
        abrir();
        rellenarObligatorios();
        // Ejecutar
        registrar();
        // Comprobar
        esperar(() -> dialogos.algunoContiene("Solicitud registrada correctamente"));
        esperar(() -> texto("pacienteId").getText().isEmpty());
        assertAll(
                () -> assertEquals(6, numeroSolicitudes()),
                () -> assertTrue(dialogos.algunoContiene("Estado: PENDIENTE_CITACION")),
                () -> assertEquals("", consultar(() -> area("indicacion").getText())));
    }

    @Test
    @DisplayName("Un seguimiento válido se registra con su periodicidad")
    void registrarSeguimiento() {
        // Preparar
        abrir();
        rellenarObligatorios();
        enEdt(() -> {
            campo(pantalla, "seguimiento", JCheckBox.class).doClick();
            texto("intervalo").setText("2");
            campo(pantalla, "unidades", JComboBox.class).setSelectedItem(UnidadPeriodicidad.SEMANAS);
            texto("repeticiones").setText("4");
            texto("solicitudOrigen").setText("1");
        });
        // Ejecutar
        registrar();
        // Comprobar
        esperar(() -> dialogos.algunoContiene("Solicitud registrada correctamente"));
        SolicitudPrueba creada = servicio.consultarSolicitud(MEDICO, 6);
        assertAll(
                () -> assertTrue(creada.esSeguimiento()),
                () -> assertEquals(2, creada.getIntervalo()),
                () -> assertEquals(UnidadPeriodicidad.SEMANAS, creada.getUnidad()),
                () -> assertEquals(1L, creada.getSolicitudOrigenId()),
                () -> assertTrue(dialogos.algunoContiene("Seguimiento: cada 2 SEMANAS, 4 repeticiones")));
    }

    @Test
    @DisplayName("Una periodicidad con formato no válido muestra el error y no se registra")
    void seguimientoNoValido() {
        // Preparar
        abrir();
        rellenarObligatorios();
        enEdt(() -> {
            campo(pantalla, "seguimiento", JCheckBox.class).doClick();
            texto("intervalo").setText("0");
            texto("repeticiones").setText("x");
        });
        // Ejecutar
        registrar();
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("El intervalo debe ser un número entero mayor que 0")),
                () -> assertEquals(5, numeroSolicitudes()));
    }

    @Test
    @DisplayName("Un ID de paciente no válido muestra el error y no se registra")
    void pacienteNoValido() {
        // Preparar
        abrir();
        rellenarObligatorios();
        enEdt(() -> texto("pacienteId").setText("-4"));
        // Ejecutar
        registrar();
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("El ID del paciente debe ser un numero positivo")),
                () -> assertEquals(5, numeroSolicitudes()));
    }

    @Test
    @DisplayName("Sin indicación clínica ni justificación se muestra el error correspondiente")
    void faltanDatosClinicos() {
        // Preparar
        abrir();
        rellenarObligatorios();
        enEdt(() -> area("indicacion").setText(" "));
        // Ejecutar y comprobar
        registrar();
        assertTrue(dialogos.algunoContiene("La indicacion clinica es obligatoria"));

        enEdt(() -> {
            area("indicacion").setText("Dolor");
            area("justificacion").setText("");
        });
        registrar();
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("La justificacion es obligatoria")),
                () -> assertEquals(5, numeroSolicitudes()));
    }

    @Test
    @DisplayName("Si el servicio rechaza la prescripción se muestra su mensaje")
    void servicioRechazaLaPrescripcion() {
        // Preparar
        SolicitudesServiceSimulado rechaza = new SolicitudesServiceSimulado(
                servicio.consultarTiposPrueba(MEDICO), List.of()) {
            @Override
            public SolicitudPrueba crearSolicitud(Usuario medico, SolicitudPrueba datos) {
                throw new java.util.NoSuchElementException("No se ha encontrado el paciente");
            }
        };
        abrir(rechaza);
        rellenarObligatorios();
        // Ejecutar
        registrar();
        // Comprobar
        esperar(() -> dialogos.algunoContiene("No se ha encontrado el paciente"));
        esperar(() -> estado().equals("No se ha registrado la solicitud"));
    }

    @Test
    @DisplayName("El botón de actualizar vuelve a cargar los tipos")
    void actualizarTipos() {
        // Preparar
        abrir();
        // Ejecutar
        enEdt(() -> boton("actualizar").doClick());
        // Comprobar
        esperar(() -> boton("actualizar").isEnabled());
        assertEquals("Tipos de prueba cargados correctamente", consultar(this::estado));
    }

    @Test
    @DisplayName("Un usuario sin rol de Médico no puede abrir la pantalla")
    void sinRolMedicoNoAbre() {
        assertThrows(IllegalArgumentException.class, () -> enEdt(() ->
                new PantallaPrescribirPrueba(servicio, new Usuario("luis", Rol.PACIENTE))));
    }

    @Test
    @DisplayName("El arranque con un rol que no es Médico muestra un aviso de permiso")
    void mainSinPermisoAvisa() {
        // Ejecutar
        PantallaPrescribirPrueba.main(new String[] {"PACIENTE"});
        // Comprobar
        assertTrue(dialogos.algunoContiene("Solo el rol Medico puede prescribir pruebas"));
    }
}
