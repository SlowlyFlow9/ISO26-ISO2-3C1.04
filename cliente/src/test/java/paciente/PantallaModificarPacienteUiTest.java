package paciente;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static soporte.Dialogos.campo;
import static soporte.Dialogos.consultar;
import static soporte.Dialogos.enEdt;
import static soporte.Dialogos.esperar;

import java.time.LocalDate;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import soporte.Dialogos;

/** CU23 y CU24 - Cliente: uso de la pantalla de modificación y baja de pacientes con el servicio simulado. */
class PantallaModificarPacienteUiTest {

    private static final Usuario ADMINISTRATIVO = new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO);

    private Dialogos dialogos;
    private PacientesServiceSimulado servicio;
    private PantallaModificarPaciente pantalla;

    @BeforeEach
    void setUp() throws Exception {
        dialogos = Dialogos.instalar();
        servicio = new PacientesServiceSimulado();
        servicio.crear(ADMINISTRATIVO, paciente("Ana", "Pérez", "12345678Z", "T1"));
        servicio.crear(ADMINISTRATIVO, paciente("Luis", "García", "87654321X", "T2"));
        enEdt(() -> {
            pantalla = new PantallaModificarPaciente(servicio, ADMINISTRATIVO);
            pantalla.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            pantalla.setVisible(true);
        });
    }

    @AfterEach
    void tearDown() {
        dialogos.close();
    }

    // ---- Utilidades ----

    private static Paciente paciente(String nombre, String apellidos, String dni, String tarjeta) {
        Paciente p = new Paciente();
        p.setNombre(nombre);
        p.setApellidos(apellidos);
        p.setDni(dni);
        p.setFechaNacimiento(LocalDate.of(1990, 5, 1));
        p.setTelefono("600111222");
        p.setNumeroTarjetaSanitaria(tarjeta);
        return p;
    }

    private JTextField texto(String nombre) { return campo(pantalla, nombre, JTextField.class); }
    private JButton boton(String nombre) { return campo(pantalla, nombre, JButton.class); }

    @SuppressWarnings("unchecked")
    private JList<String> lista() { return campo(pantalla, "lista", JList.class); }

    @SuppressWarnings("unchecked")
    private JComboBox<String> combo(String nombre) { return campo(pantalla, nombre, JComboBox.class); }

    private void buscar(String criterio) {
        enEdt(() -> {
            texto("criterio").setText(criterio);
            Dialogos.boton(pantalla, "Buscar").doClick();
        });
    }

    private void buscarYSeleccionar(String criterio) {
        buscar(criterio);
        enEdt(() -> lista().setSelectedIndex(0));
    }

    // ---- Búsqueda ----

    @Test
    @DisplayName("Buscar sin criterio muestra un aviso")
    void buscarSinCriterio() {
        // Ejecutar
        buscar(" ");
        // Comprobar
        assertTrue(dialogos.algunoContiene("Escribe un nombre, unos apellidos o un DNI para buscar"));
    }

    @Test
    @DisplayName("Una búsqueda sin resultados lo indica")
    void buscarSinResultados() {
        // Ejecutar
        buscar("Zacarías");
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("No se ha encontrado ningún paciente")),
                () -> assertEquals(0, consultar(() -> lista().getModel().getSize())));
    }

    @Test
    @DisplayName("Al seleccionar un paciente el formulario se abre con sus datos")
    void seleccionarPaciente() {
        // Ejecutar
        buscarYSeleccionar("Ana");
        // Comprobar
        assertAll(
                () -> assertEquals("Ana Pérez  -  12345678Z", consultar(() -> lista().getModel().getElementAt(0))),
                () -> assertEquals("Ana", consultar(() -> texto("nombre").getText())),
                () -> assertEquals("1990", consultar(() -> (String) combo("anio").getSelectedItem())),
                () -> assertTrue(consultar(() -> boton("guardar").isEnabled())),
                () -> assertTrue(consultar(() -> boton("darDeBaja").isEnabled())));
    }

    // ---- Modificación ----

    @Test
    @DisplayName("Guardar cambios válidos actualiza el paciente y la lista")
    void guardarCambios() throws Exception {
        // Preparar
        buscarYSeleccionar("Ana");
        enEdt(() -> texto("telefono").setText("699000111"));
        // Ejecutar
        enEdt(() -> boton("guardar").doClick());
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("Datos actualizados correctamente")),
                () -> assertEquals("699000111", servicio.consultar(ADMINISTRATIVO, 1).getTelefono()));
    }

    @Test
    @DisplayName("Datos con formato no válido muestran el error y no se guardan")
    void guardarDatosNoValidos() throws Exception {
        // Preparar
        buscarYSeleccionar("Ana");
        enEdt(() -> texto("dni").setText("123"));
        // Ejecutar
        enEdt(() -> boton("guardar").doClick());
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("El DNI/NIE no tiene un formato válido")),
                () -> assertEquals("12345678Z", servicio.consultar(ADMINISTRATIVO, 1).getDni()));
    }

    @Test
    @DisplayName("Una fecha de nacimiento que no existe se rechaza")
    void fechaQueNoExiste() {
        // Preparar: 31 de abril
        buscarYSeleccionar("Ana");
        enEdt(() -> {
            combo("dia").setSelectedIndex(31);
            combo("mes").setSelectedIndex(4);
        });
        // Ejecutar
        enEdt(() -> boton("guardar").doClick());
        // Comprobar
        assertTrue(dialogos.algunoContiene("La fecha de nacimiento no existe"));
    }

    @Test
    @DisplayName("Un DNI de otro paciente muestra el error de duplicado")
    void guardarDniDuplicado() {
        // Preparar
        buscarYSeleccionar("Ana");
        enEdt(() -> texto("dni").setText("87654321X"));
        // Ejecutar
        enEdt(() -> boton("guardar").doClick());
        // Comprobar
        assertTrue(dialogos.algunoContiene("Ya existe un paciente con el DNI 87654321X"));
    }

    @Test
    @DisplayName("Si el paciente se dio de baja mientras se editaba, se bloquea el formulario")
    void pacienteDadoDeBajaMientrasSeEdita() throws Exception {
        // Preparar
        buscarYSeleccionar("Ana");
        servicio.desactivar(ADMINISTRATIVO, 1);
        // Ejecutar
        enEdt(() -> boton("guardar").doClick());
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("está dado de baja")),
                () -> assertFalse(consultar(() -> boton("guardar").isEnabled())),
                () -> assertEquals("Este paciente está dado de baja y no se puede modificar",
                        consultar(() -> campo(pantalla, "aviso", JLabel.class).getText())));
    }

    // ---- Baja ----

    @Test
    @DisplayName("Dar de baja con confirmación quita al paciente de la lista")
    void darDeBaja() throws Exception {
        // Preparar
        buscarYSeleccionar("Luis");
        // Ejecutar
        enEdt(() -> boton("darDeBaja").doClick());
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("¿Quieres dar de baja a Luis García")),
                () -> assertTrue(dialogos.algunoContiene("El paciente se ha dado de baja correctamente.")),
                () -> assertFalse(servicio.consultar(ADMINISTRATIVO, 2).isActivo()),
                () -> assertEquals(0, consultar(() -> lista().getModel().getSize())));
    }

    @Test
    @DisplayName("Si no se confirma la baja el paciente sigue activo")
    void darDeBajaSinConfirmar() throws Exception {
        // Preparar
        buscarYSeleccionar("Luis");
        dialogos.responder(Dialogos::no);
        // Ejecutar
        enEdt(() -> boton("darDeBaja").doClick());
        // Comprobar
        assertTrue(servicio.consultar(ADMINISTRATIVO, 2).isActivo());
    }

    @Test
    @DisplayName("Si el paciente ya estaba de baja se muestra el error")
    void darDeBajaPacienteYaInactivo() throws Exception {
        // Preparar
        buscarYSeleccionar("Luis");
        servicio.desactivar(ADMINISTRATIVO, 2);
        // Ejecutar
        enEdt(() -> boton("darDeBaja").doClick());
        // Comprobar
        assertTrue(dialogos.algunoContiene("No se puede dar de baja"));
    }

    // ---- Arranque ----

    @Test
    @DisplayName("El arranque con un rol sin permiso muestra un aviso")
    void mainSinPermiso() {
        // Ejecutar
        PantallaModificarPaciente.main(new String[] {"MEDICO"});
        // Comprobar
        assertTrue(dialogos.algunoContiene("El rol MEDICO no puede modificar pacientes"));
    }

    @Test
    @DisplayName("El arranque con el servicio simulado abre la pantalla con pacientes de ejemplo")
    void mainConServicioSimulado() {
        // Ejecutar
        PantallaModificarPaciente.main(new String[] {"PERSONAL_ADMINISTRATIVO", "simulado"});
        // Comprobar
        esperar(() -> java.util.Arrays.stream(java.awt.Window.getWindows())
                .anyMatch(v -> v instanceof PantallaModificarPaciente && v != pantalla && v.isShowing()));
        enEdt(() -> java.util.Arrays.stream(java.awt.Window.getWindows())
                .filter(v -> v instanceof PantallaModificarPaciente)
                .forEach(v -> ((PantallaModificarPaciente) v).setDefaultCloseOperation(
                        javax.swing.WindowConstants.DISPOSE_ON_CLOSE)));
    }
}
