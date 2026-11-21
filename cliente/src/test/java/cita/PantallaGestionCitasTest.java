package cita;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Paciente;
import paciente.PacientesServiceSimulado;
import paciente.Rol;
import paciente.Usuario;
import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;

/** CU25: pruebas de la pantalla de gestión de citas. */
class PantallaGestionCitasTest {

    private static final Usuario ADMIN =
            new Usuario("admin", Rol.PERSONAL_ADMINISTRATIVO);
    private static final LocalDate DIA = LocalDate.of(2026, 12, 1);

    private PacientesServiceSimulado pacientes;
    private CitasAdministracionServiceSimulado servicio;
    private AtomicBoolean respuesta;
    private Paciente ana;
    private Paciente luis;
    private PantallaGestionCitas pantalla;

    @BeforeEach
    void setUp() throws Exception {

        pacientes = new PacientesServiceSimulado();
        servicio = new CitasAdministracionServiceSimulado(pacientes);
        respuesta = new AtomicBoolean(true);

        ana = crearPaciente("Ana", "García", "12345678Z");
        luis = crearPaciente("Luis", "Martín", "87654321X");

        servicio.agregarSolicitud(solicitud(1, ana.getId()));
        servicio.agregarSolicitud(solicitud(2, ana.getId()));

        servicio.agregarHueco(1, DIA, LocalTime.of(9, 0),
                LocalTime.of(9, 30), 7, false, true);
        servicio.agregarHueco(2, DIA, LocalTime.of(9, 30),
                LocalTime.of(10, 0), 7, false, true);
        servicio.agregarHueco(3, DIA, LocalTime.of(10, 0),
                LocalTime.of(10, 30), 7, true, true);

        pantalla = crearPantalla(ADMIN);
    }

    // ---------- utilidades ----------

    private Paciente crearPaciente(
            String nombre, String apellidos, String dni)
            throws Exception {
        Paciente datos = new Paciente();
        datos.setNombre(nombre);
        datos.setApellidos(apellidos);
        datos.setDni(dni);
        datos.setFechaNacimiento(LocalDate.of(1980, 5, 20));
        datos.setTelefono("600123456");
        return pacientes.crear(ADMIN, datos);
    }

    private SolicitudPrueba solicitud(long id, long pacienteId) {
        return new SolicitudPrueba(
                id, pacienteId, 5, 3, "Dolor", "Control",
                Prioridad.ORDINARIA,
                LocalDateTime.of(2026, 11, 20, 10, 0),
                EstadoSolicitud.PENDIENTE_CITACION);
    }

    private PantallaGestionCitas crearPantalla(Usuario usuario)
            throws Exception {

        PantallaGestionCitas[] creada = new PantallaGestionCitas[1];

        SwingUtilities.invokeAndWait(() ->
                creada[0] = new PantallaGestionCitas(
                        servicio, pacientes, usuario,
                        texto -> respuesta.get()));

        return creada[0];
    }

    private <T extends Component> T componente(
            String nombre, Class<T> tipo) {
        return buscar(pantalla, nombre, tipo);
    }

    private static <T extends Component> T buscar(
            Container contenedor, String nombre, Class<T> tipo) {

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

    private void enHilo(Runnable accion) throws Exception {
        SwingUtilities.invokeAndWait(accion);
    }

    private void buscarPaciente(String criterio) throws Exception {
        enHilo(() -> {
            componente("campoBusqueda", JTextField.class)
                    .setText(criterio);
            componente("botonBuscar", JButton.class).doClick();
        });
    }

    private void seleccionarFila(String tabla, int fila)
            throws Exception {
        enHilo(() -> componente(tabla, JTable.class)
                .setRowSelectionInterval(fila, fila));
    }

    private void seleccionarAna() throws Exception {
        buscarPaciente("12345678Z");
        seleccionarFila("tablaPacientes", 0);
    }

    private void pulsar(String boton) throws Exception {
        enHilo(() -> componente(boton, JButton.class).doClick());
    }

    private String mensaje() {
        return componente("mensaje", JLabel.class).getText();
    }

    private int filas(String tabla) {
        return componente(tabla, JTable.class).getRowCount();
    }

    private int opciones(String combo) {
        return componente(combo, JComboBox.class).getItemCount();
    }

    // ---------- búsqueda de paciente ----------

    @Test
    @DisplayName("Buscar por DNI muestra el paciente encontrado")
    void buscarMuestraElPaciente() throws Exception {
        // Ejecutar
        buscarPaciente("12345678Z");
        // Comprobar
        assertAll(
                () -> assertEquals(1, filas("tablaPacientes")),
                () -> assertEquals("1 paciente(s) encontrado(s)", mensaje()));
    }

    @Test
    @DisplayName("Buscar sin escribir nada pide un criterio y no muestra pacientes")
    void buscarSinCriterio() throws Exception {
        // Ejecutar
        buscarPaciente("  ");
        // Comprobar
        assertAll(
                () -> assertEquals(0, filas("tablaPacientes")),
                () -> assertEquals(
                        "Escribe un nombre o un DNI para buscar",
                        mensaje()));
    }

    @Test
    @DisplayName("Buscar un paciente que no existe lo indica")
    void buscarSinResultados() throws Exception {
        // Ejecutar
        buscarPaciente("00000000T");
        // Comprobar
        assertAll(
                () -> assertEquals(0, filas("tablaPacientes")),
                () -> assertEquals(
                        "No se ha encontrado ningún paciente",
                        mensaje()));
    }

    // ---------- selección de paciente ----------

    @Test
    @DisplayName("Seleccionar un paciente carga sus solicitudes pendientes y los huecos libres")
    void seleccionarCargaLasListas() throws Exception {
        // Ejecutar
        seleccionarAna();
        // Comprobar
        assertAll(
                () -> assertEquals(2, opciones("comboSolicitudes")),
                () -> assertEquals(2, opciones("comboHuecos")),
                () -> assertEquals("Paciente seleccionado", mensaje()));
    }

    @Test
    @DisplayName("Seleccionar un paciente muestra sus citas")
    void seleccionarMuestraSusCitas() throws Exception {
        // Preparar
        servicio.asignar(ADMIN, 1, 1);
        // Ejecutar
        seleccionarAna();
        // Comprobar
        assertEquals(1, filas("tablaCitas"));
    }

    @Test
    @DisplayName("Una nueva búsqueda borra las citas del paciente anterior")
    void nuevaBusquedaLimpia() throws Exception {
        // Preparar
        servicio.asignar(ADMIN, 1, 1);
        seleccionarAna();
        // Ejecutar
        buscarPaciente("87654321X");
        // Comprobar
        assertAll(
                () -> assertEquals(0, filas("tablaCitas")),
                () -> assertEquals(0, opciones("comboSolicitudes")));
    }

    // ---------- asignar ----------

    @Test
    @DisplayName("Asignar crea la cita y actualiza la tabla y las listas")
    void asignarCreaLaCita() throws Exception {
        // Preparar
        seleccionarAna();
        // Ejecutar
        pulsar("botonAsignar");
        // Comprobar
        assertAll(
                () -> assertEquals(1, filas("tablaCitas")),
                () -> assertEquals(1, opciones("comboSolicitudes")),
                () -> assertEquals(1, opciones("comboHuecos")),
                () -> assertTrue(mensaje().startsWith("Cita 1 asignada")));
    }

    @Test
    @DisplayName("Asignar sin paciente seleccionado pide elegir uno")
    void asignarSinPaciente() throws Exception {
        // Ejecutar
        pulsar("botonAsignar");
        // Comprobar
        assertEquals("Selecciona primero un paciente", mensaje());
    }

    @Test
    @DisplayName("Asignar a un paciente sin solicitudes pendientes lo indica")
    void asignarSinSolicitudes() throws Exception {
        // Preparar
        buscarPaciente("87654321X");
        seleccionarFila("tablaPacientes", 0);
        // Ejecutar
        pulsar("botonAsignar");
        // Comprobar
        assertEquals(
                "El paciente no tiene solicitudes pendientes de citación",
                mensaje());
    }

    @Test
    @DisplayName("Asignar sin huecos libres lo indica")
    void asignarSinHuecos() throws Exception {
        // Preparar
        servicio.asignar(ADMIN, 1, 1);
        servicio.asignar(ADMIN, 2, 2);
        servicio.agregarSolicitud(solicitud(3, ana.getId()));
        seleccionarAna();
        // Ejecutar
        pulsar("botonAsignar");
        // Comprobar
        assertEquals("No hay huecos libres para dar la cita", mensaje());
    }

    @Test
    @DisplayName("Si el hueco se ocupó mientras tanto, se muestra el error del servicio")
    void asignarMuestraElErrorDelServicio() throws Exception {
        // Preparar: la pantalla muestra el hueco 1, pero otra persona lo ocupa
        seleccionarAna();
        servicio.asignar(ADMIN, 2, 1);
        // Ejecutar
        pulsar("botonAsignar");
        // Comprobar
        assertEquals("Error: El hueco 1 ya tiene una cita", mensaje());
    }

    // ---------- reprogramar ----------

    @Test
    @DisplayName("Reprogramar mueve la cita seleccionada al hueco elegido")
    void reprogramarMueveLaCita() throws Exception {
        // Preparar
        servicio.asignar(ADMIN, 1, 1);
        seleccionarAna();
        seleccionarFila("tablaCitas", 0);
        // Ejecutar (el único hueco libre es el 2)
        pulsar("botonReprogramar");
        // Comprobar
        assertAll(
                () -> assertEquals(2L, servicio
                        .consultarCita(ADMIN, 1).getHuecoId()),
                () -> assertTrue(mensaje().startsWith(
                        "Cita 1 reprogramada")));
    }

    @Test
    @DisplayName("Reprogramar sin cita seleccionada pide elegir una")
    void reprogramarSinCita() throws Exception {
        // Preparar
        seleccionarAna();
        // Ejecutar
        pulsar("botonReprogramar");
        // Comprobar
        assertEquals("Selecciona primero una cita", mensaje());
    }

    @Test
    @DisplayName("Reprogramar sin huecos libres lo indica")
    void reprogramarSinHuecos() throws Exception {
        // Preparar
        servicio.asignar(ADMIN, 1, 1);
        servicio.asignar(ADMIN, 2, 2);
        seleccionarAna();
        seleccionarFila("tablaCitas", 0);
        // Ejecutar
        pulsar("botonReprogramar");
        // Comprobar
        assertEquals("No hay huecos libres para reprogramar", mensaje());
    }

    // ---------- cancelar ----------

    @Test
    @DisplayName("Cancelar con confirmación cancela la cita y la solicitud vuelve a la lista")
    void cancelarConfirmado() throws Exception {
        // Preparar
        servicio.asignar(ADMIN, 1, 1);
        seleccionarAna();
        seleccionarFila("tablaCitas", 0);
        // Ejecutar
        pulsar("botonCancelar");
        // Comprobar
        assertAll(
                () -> assertEquals(EstadoCita.CANCELADA,
                        servicio.consultarCita(ADMIN, 1).getEstado()),
                () -> assertEquals(2, opciones("comboSolicitudes")),
                () -> assertEquals("Cita 1 cancelada", mensaje()));
    }

    @Test
    @DisplayName("Si no se confirma, la cita no se cancela")
    void cancelarSinConfirmar() throws Exception {
        // Preparar
        servicio.asignar(ADMIN, 1, 1);
        seleccionarAna();
        seleccionarFila("tablaCitas", 0);
        respuesta.set(false);
        // Ejecutar
        pulsar("botonCancelar");
        // Comprobar
        assertAll(
                () -> assertEquals(EstadoCita.PROGRAMADA,
                        servicio.consultarCita(ADMIN, 1).getEstado()),
                () -> assertEquals("Cancelación anulada", mensaje()));
    }

    @Test
    @DisplayName("Cancelar sin cita seleccionada pide elegir una")
    void cancelarSinCita() throws Exception {
        // Preparar
        seleccionarAna();
        // Ejecutar
        pulsar("botonCancelar");
        // Comprobar
        assertEquals("Selecciona primero una cita", mensaje());
    }

    @Test
    @DisplayName("Cancelar una cita que ya no se puede modificar muestra el error del servicio")
    void cancelarCitaNoModificable() throws Exception {
        // Preparar
        Cita cita = servicio.asignar(ADMIN, 1, 1).copia();
        seleccionarAna();
        seleccionarFila("tablaCitas", 0);
        cita.setEstado(EstadoCita.REALIZADA);
        servicio.agregarCita(cita);
        // Ejecutar
        pulsar("botonCancelar");
        // Comprobar
        assertTrue(mensaje().startsWith("Error: La cita 1 está REALIZADA"));
    }

    // ---------- permisos ----------

    @Test
    @DisplayName("Un médico no ve las acciones de gestión y se le avisa")
    void medicoNoVeLasAcciones() throws Exception {
        // Ejecutar
        pantalla = crearPantalla(new Usuario("medico", Rol.MEDICO));
        // Comprobar
        assertAll(
                () -> assertNull(componente("botonAsignar", JButton.class)),
                () -> assertNull(componente("botonCancelar", JButton.class)),
                () -> assertEquals(
                        "Esta pantalla solo está disponible para el "
                                + "personal administrativo",
                        mensaje()));
    }

    @Test
    @DisplayName("Solo el personal administrativo tiene disponible la pantalla")
    void disponibleSoloParaPersonalAdministrativo() {
        // Comprobar
        assertAll(
                () -> assertTrue(
                        PantallaGestionCitas.disponiblePara(ADMIN)),
                () -> assertFalse(PantallaGestionCitas.disponiblePara(
                        new Usuario("p", Rol.PACIENTE))),
                () -> assertFalse(PantallaGestionCitas.disponiblePara(
                        new Usuario("m", Rol.MEDICO))),
                () -> assertFalse(PantallaGestionCitas.disponiblePara(
                        new Usuario("a", Rol.ADMINISTRADOR))),
                () -> assertFalse(
                        PantallaGestionCitas.disponiblePara(null)));
    }
}
