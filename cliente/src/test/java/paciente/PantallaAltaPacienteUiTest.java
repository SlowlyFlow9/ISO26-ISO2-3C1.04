package paciente;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static soporte.Dialogos.campo;
import static soporte.Dialogos.consultar;
import static soporte.Dialogos.enEdt;
import static soporte.Dialogos.esperar;

import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JTextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import soporte.Dialogos;

/** CU22 - Cliente: uso de la pantalla de alta de paciente con el servicio simulado. */
class PantallaAltaPacienteUiTest {

    private static final Usuario ADMINISTRATIVO = new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO);

    private Dialogos dialogos;
    private PacientesServiceSimulado servicio;
    private PantallaAltaPaciente pantalla;

    @BeforeEach
    void setUp() {
        dialogos = Dialogos.instalar();
        servicio = new PacientesServiceSimulado();
    }

    @AfterEach
    void tearDown() {
        dialogos.close();
    }

    // ---- Utilidades ----

    private void abrir(PacientesService servicioPantalla, Usuario usuario) {
        enEdt(() -> {
            pantalla = new PantallaAltaPaciente(servicioPantalla, usuario);
            pantalla.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
            pantalla.setVisible(true);
        });
    }

    private JTextField texto(String nombre) { return campo(pantalla, nombre, JTextField.class); }

    @SuppressWarnings("unchecked")
    private JComboBox<String> combo(String nombre) { return campo(pantalla, nombre, JComboBox.class); }

    /** Rellena un paciente válido; la fecha se indica con los índices de día y mes y el año. */
    private void rellenar(String dni, int dia, int mes, String anio) {
        enEdt(() -> {
            texto("nombre").setText("Lucía");
            texto("apellidos").setText("García Pérez");
            texto("dni").setText(dni);
            combo("dia").setSelectedIndex(dia);
            combo("mes").setSelectedIndex(mes);
            combo("anio").setSelectedItem(anio);
            texto("telefono").setText("600123123");
            texto("email").setText("lucia@correo.es");
        });
    }

    private void darDeAlta() {
        enEdt(() -> Dialogos.boton(pantalla, "Dar de alta").doClick());
    }

    // ---- Alta ----

    @Test
    @DisplayName("Un paciente válido se da de alta, se muestra el resumen y se limpia el formulario")
    void altaValida() {
        // Preparar
        abrir(servicio, ADMINISTRATIVO);
        rellenar("12345678Z", 15, 3, "1990");
        // Ejecutar
        darDeAlta();
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("Paciente dado de alta")),
                () -> assertTrue(dialogos.algunoContiene("Fecha de nacimiento: 1990-03-15")),
                () -> assertTrue(dialogos.algunoContiene("Tarjeta sanitaria: -")),
                () -> assertEquals(1, servicio.buscar(ADMINISTRATIVO, "Lucía").size()),
                () -> assertEquals("", consultar(() -> texto("nombre").getText())),
                () -> assertEquals(0, consultar(() -> combo("dia").getSelectedIndex())));
    }

    @Test
    @DisplayName("Sin datos obligatorios se muestran los errores y no se da de alta")
    void faltanDatosObligatorios() {
        // Preparar
        abrir(servicio, ADMINISTRATIVO);
        // Ejecutar
        darDeAlta();
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("El nombre es obligatorio")),
                () -> assertTrue(dialogos.algunoContiene("La fecha de nacimiento es obligatoria")),
                () -> assertEquals(0, servicio.buscar(ADMINISTRATIVO, "a").size()));
    }

    @Test
    @DisplayName("Una fecha de nacimiento que no existe se rechaza")
    void fechaQueNoExiste() {
        // Preparar: 31 de febrero
        abrir(servicio, ADMINISTRATIVO);
        rellenar("12345678Z", 31, 2, "2000");
        // Ejecutar
        darDeAlta();
        // Comprobar
        assertTrue(dialogos.algunoContiene("La fecha de nacimiento no existe"));
    }

    @Test
    @DisplayName("Un DNI ya registrado muestra el error de duplicado")
    void dniDuplicado() {
        // Preparar
        abrir(servicio, ADMINISTRATIVO);
        rellenar("12345678Z", 1, 1, "1980");
        darDeAlta();
        rellenar("12345678Z", 2, 2, "1985");
        // Ejecutar
        darDeAlta();
        // Comprobar
        assertTrue(dialogos.algunoContiene("Ya existe un paciente con el DNI 12345678Z"));
    }

    @Test
    @DisplayName("Si el usuario no tiene permiso se muestra el error del servicio")
    void sinPermiso() {
        // Preparar
        abrir(servicio, new Usuario("medico", Rol.MEDICO));
        rellenar("12345678Z", 1, 1, "1980");
        // Ejecutar
        darDeAlta();
        // Comprobar
        assertTrue(dialogos.algunoContiene("Solo el Personal administrativo puede dar de alta pacientes"));
    }

    @Test
    @DisplayName("Si el servidor no responde o rechaza los datos se muestra su mensaje")
    void erroresDelServicio() {
        // Preparar
        PacientesServiceSimulado caido = new PacientesServiceSimulado() {
            private int llamadas;

            @Override
            public Paciente crear(Usuario solicitante, Paciente datos)
                    throws SinPermisoException, DatosInvalidosException, PacienteDuplicadoException {
                if (llamadas++ == 0) throw new ServicioNoDisponibleException("No se pudo conectar con el servidor");
                throw new DatosInvalidosException(List.of("La tarjeta sanitaria no es válida"));
            }
        };
        abrir(caido, ADMINISTRATIVO);
        rellenar("12345678Z", 1, 1, "1980");
        // Ejecutar
        darDeAlta();
        darDeAlta();
        // Comprobar
        assertAll(
                () -> assertTrue(dialogos.algunoContiene("No se pudo conectar con el servidor")),
                () -> assertTrue(dialogos.algunoContiene("La tarjeta sanitaria no es válida")));
    }

    // ---- Arranque ----

    @Test
    @DisplayName("El arranque con un rol sin permiso muestra un aviso")
    void mainSinPermiso() {
        // Ejecutar
        PantallaAltaPaciente.main(new String[] {"MEDICO"});
        // Comprobar
        assertTrue(dialogos.algunoContiene("El rol MEDICO no puede dar de alta pacientes"));
    }

    @Test
    @DisplayName("El arranque con el servicio simulado abre la pantalla")
    void mainConServicioSimulado() {
        // Ejecutar
        PantallaAltaPaciente.main(new String[] {"PERSONAL_ADMINISTRATIVO", "simulado"});
        // Comprobar
        esperar(() -> java.util.Arrays.stream(java.awt.Window.getWindows())
                .anyMatch(v -> v instanceof PantallaAltaPaciente && v.isShowing()));
        enEdt(() -> java.util.Arrays.stream(java.awt.Window.getWindows())
                .filter(v -> v instanceof PantallaAltaPaciente)
                .forEach(v -> ((PantallaAltaPaciente) v).setDefaultCloseOperation(
                        javax.swing.WindowConstants.DISPOSE_ON_CLOSE)));
    }
}
