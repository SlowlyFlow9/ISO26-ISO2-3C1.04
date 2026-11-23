package cita;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import paciente.Paciente;
import paciente.PacientesService;
import paciente.Rol;
import paciente.Usuario;
import solicitud.SolicitudPrueba;

/**
 * CU25: pantalla con la que el personal administrativo gestiona las citas
 * de un paciente. No contiene lógica de negocio: valida que se haya elegido
 * lo necesario y muestra los errores que devuelve el servicio (D-013).
 */
public class PantallaGestionCitas
        extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Pregunta de confirmación; se puede sustituir en las pruebas. */
    @FunctionalInterface
    public interface Confirmador {
        boolean confirmar(String mensaje);
    }

    /** Elemento de un desplegable: un texto y el valor que representa. */
    private static final class Opcion<T> {

        private final String texto;
        private final T valor;

        Opcion(String texto, T valor) {
            this.texto = texto;
            this.valor = valor;
        }

        @Override
        public String toString() {
            return texto;
        }
    }

    private final CitasAdministracionService servicio;
    private final PacientesService pacientesServicio;
    private final Usuario usuario;
    private final Confirmador confirmador;

    private final JTextField campoBusqueda = new JTextField(20);
    private final JButton botonBuscar = new JButton("Buscar");

    private final DefaultTableModel modeloPacientes =
            tabla("ID", "Paciente", "DNI", "Estado");
    private final JTable tablaPacientes = new JTable(modeloPacientes);

    private final DefaultTableModel modeloCitas =
            tabla("ID", "Fecha y hora", "Médico", "Tipo de prueba",
                    "Estado");
    private final JTable tablaCitas = new JTable(modeloCitas);

    private final JComboBox<Opcion<SolicitudPrueba>> comboSolicitudes =
            new JComboBox<>();
    private final JComboBox<Opcion<HuecoDisponible>> comboHuecos =
            new JComboBox<>();

    private final JButton botonAsignar = new JButton("Asignar cita");
    private final JButton botonReprogramar =
            new JButton("Reprogramar a este hueco");
    private final JButton botonCancelar = new JButton("Cancelar cita");

    private final JLabel mensaje = new JLabel(" ");

    private final List<Paciente> pacientesMostrados = new ArrayList<>();
    private final List<Cita> citasMostradas = new ArrayList<>();
    private Long pacienteSeleccionado;

    public PantallaGestionCitas(
            CitasAdministracionService servicio,
            PacientesService pacientesServicio,
            Usuario usuario) {
        this(servicio, pacientesServicio, usuario, null);
    }

    public PantallaGestionCitas(
            CitasAdministracionService servicio,
            PacientesService pacientesServicio,
            Usuario usuario,
            Confirmador confirmador) {

        this.servicio = servicio;
        this.pacientesServicio = pacientesServicio;
        this.usuario = usuario;
        this.confirmador = confirmador != null
                ? confirmador
                : texto -> JOptionPane.showConfirmDialog(
                        this, texto, "Confirmar",
                        JOptionPane.YES_NO_OPTION)
                        == JOptionPane.YES_OPTION;

        construir();
    }

    /** Solo el personal administrativo ve las acciones (D-016). */
    public static boolean disponiblePara(Usuario usuario) {
        return usuario != null
                && usuario.rol() == Rol.PERSONAL_ADMINISTRATIVO;
    }

    private static DefaultTableModel tabla(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    // ---------- construcción de la pantalla ----------

    private void construir() {

        setLayout(new BorderLayout(10, 10));

        if (!disponiblePara(usuario)) {
            mensaje.setName("mensaje");
            mensaje.setText("Esta pantalla solo está disponible "
                    + "para el personal administrativo");
            add(mensaje, BorderLayout.CENTER);
            return;
        }

        campoBusqueda.setName("campoBusqueda");
        botonBuscar.setName("botonBuscar");
        tablaPacientes.setName("tablaPacientes");
        tablaCitas.setName("tablaCitas");
        comboSolicitudes.setName("comboSolicitudes");
        comboHuecos.setName("comboHuecos");
        botonAsignar.setName("botonAsignar");
        botonReprogramar.setName("botonReprogramar");
        botonCancelar.setName("botonCancelar");
        mensaje.setName("mensaje");

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT));
        busqueda.add(new JLabel("Buscar paciente (nombre o DNI):"));
        busqueda.add(campoBusqueda);
        busqueda.add(botonBuscar);
        add(busqueda, BorderLayout.NORTH);

        tablaPacientes.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        tablaCitas.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        JPanel centro = new JPanel(new GridLayout(2, 1, 5, 5));
        centro.add(conTitulo("Pacientes encontrados", tablaPacientes));
        centro.add(conTitulo("Citas del paciente", tablaCitas));
        add(centro, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new GridLayout(0, 1, 5, 5));
        acciones.add(fila("Solicitud pendiente:", comboSolicitudes));
        acciones.add(fila("Hueco libre:", comboHuecos));

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botones.add(botonAsignar);
        botones.add(botonReprogramar);
        botones.add(botonCancelar);
        acciones.add(botones);
        acciones.add(mensaje);
        add(acciones, BorderLayout.SOUTH);

        botonBuscar.addActionListener(e -> buscar());
        campoBusqueda.addActionListener(e -> buscar());
        botonAsignar.addActionListener(e -> asignar());
        botonReprogramar.addActionListener(e -> reprogramar());
        botonCancelar.addActionListener(e -> cancelar());

        tablaPacientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                seleccionarPaciente();
            }
        });
    }

    private JPanel conTitulo(String titulo, JTable tabla) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(titulo), BorderLayout.NORTH);
        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);
        return panel;
    }

    private JPanel fila(String texto, JComboBox<?> combo) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(new JLabel(texto), BorderLayout.WEST);
        panel.add(combo, BorderLayout.CENTER);
        return panel;
    }

    // ---------- acciones ----------

    private void buscar() {

        String criterio = campoBusqueda.getText().trim();

        limpiarPaciente();
        modeloPacientes.setRowCount(0);
        pacientesMostrados.clear();

        if (criterio.isEmpty()) {
            mostrar("Escribe un nombre o un DNI para buscar");
            return;
        }

        List<Paciente> encontrados =
                pacientesServicio.buscar(usuario, criterio);

        for (Paciente p : encontrados) {
            pacientesMostrados.add(p);
            modeloPacientes.addRow(new Object[] {
                    p.getId(),
                    p.getNombre() + " " + p.getApellidos(),
                    p.getDni(),
                    p.isActivo() ? "Activo" : "De baja"
            });
        }

        mostrar(encontrados.isEmpty()
                ? "No se ha encontrado ningún paciente"
                : encontrados.size() + " paciente(s) encontrado(s)");
    }

    private void seleccionarPaciente() {

        int fila = tablaPacientes.getSelectedRow();

        if (fila < 0 || fila >= pacientesMostrados.size()) {
            return;
        }

        pacienteSeleccionado = pacientesMostrados.get(fila).getId();
        refrescar();
        mostrar("Paciente seleccionado");
    }

    private void asignar() {

        if (pacienteSeleccionado == null) {
            mostrar("Selecciona primero un paciente");
            return;
        }

        Opcion<SolicitudPrueba> solicitud = elegida(comboSolicitudes);
        Opcion<HuecoDisponible> hueco = elegida(comboHuecos);

        if (solicitud == null) {
            mostrar("El paciente no tiene solicitudes pendientes "
                    + "de citación");
            return;
        }
        if (hueco == null) {
            mostrar("No hay huecos libres para dar la cita");
            return;
        }

        try {
            Cita cita = servicio.asignar(
                    usuario,
                    solicitud.valor.getId(),
                    hueco.valor.id());
            refrescar();
            mostrar("Cita " + cita.getId() + " asignada para el "
                    + cita.getFechaHora().format(FORMATO));
        } catch (Exception e) {
            mostrar("Error: " + e.getMessage());
        }
    }

    private void reprogramar() {

        Cita cita = citaSeleccionada();

        if (cita == null) {
            mostrar("Selecciona primero una cita");
            return;
        }

        Opcion<HuecoDisponible> hueco = elegida(comboHuecos);

        if (hueco == null) {
            mostrar("No hay huecos libres para reprogramar");
            return;
        }

        try {
            Cita movida = servicio.reprogramar(
                    usuario, cita.getId(), hueco.valor.id());
            refrescar();
            mostrar("Cita " + movida.getId()
                    + " reprogramada para el "
                    + movida.getFechaHora().format(FORMATO));
        } catch (Exception e) {
            mostrar("Error: " + e.getMessage());
        }
    }

    private void cancelar() {

        Cita cita = citaSeleccionada();

        if (cita == null) {
            mostrar("Selecciona primero una cita");
            return;
        }

        if (!confirmador.confirmar(
                "¿Seguro que quieres cancelar la cita "
                        + cita.getId() + "?")) {
            mostrar("Cancelación anulada");
            return;
        }

        try {
            servicio.cancelar(usuario, cita.getId());
            refrescar();
            mostrar("Cita " + cita.getId() + " cancelada");
        } catch (Exception e) {
            mostrar("Error: " + e.getMessage());
        }
    }

    // ---------- carga de datos ----------

    // vuelve a leer del servicio las citas del paciente y las listas de elección
    private void refrescar() {

        modeloCitas.setRowCount(0);
        citasMostradas.clear();
        comboSolicitudes.removeAllItems();
        comboHuecos.removeAllItems();

        try {
            for (Cita c : servicio.listarCitasDePaciente(
                    usuario, pacienteSeleccionado)) {
                citasMostradas.add(c);
                modeloCitas.addRow(new Object[] {
                        c.getId(),
                        c.getFechaHora().format(FORMATO),
                        c.getMedicoId(),
                        c.getTipoPruebaId(),
                        c.getEstado()
                });
            }

            for (SolicitudPrueba s : servicio.listarSolicitudesPendientes(
                    usuario, pacienteSeleccionado)) {
                comboSolicitudes.addItem(new Opcion<>(
                        "Solicitud " + s.getId() + " · "
                                + s.getPrioridad() + " · "
                                + s.getIndicacionClinica(), s));
            }

            for (HuecoDisponible h : servicio.listarHuecosLibres(usuario)) {
                comboHuecos.addItem(new Opcion<>(h.toString(), h));
            }
        } catch (Exception e) {
            mostrar("Error: " + e.getMessage());
        }
    }

    private void limpiarPaciente() {
        pacienteSeleccionado = null;
        modeloCitas.setRowCount(0);
        citasMostradas.clear();
        comboSolicitudes.removeAllItems();
        comboHuecos.removeAllItems();
    }

    private static <T> Opcion<T> elegida(JComboBox<Opcion<T>> combo) {

        int indice = combo.getSelectedIndex();

        return indice < 0 ? null : combo.getItemAt(indice);
    }

    private Cita citaSeleccionada() {

        int fila = tablaCitas.getSelectedRow();

        if (fila < 0 || fila >= citasMostradas.size()) {
            return null;
        }
        return citasMostradas.get(fila);
    }

    private void mostrar(String texto) {
        mensaje.setText(texto);
    }
}
