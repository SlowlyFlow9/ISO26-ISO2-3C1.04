package solicitud;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU28 - Pantalla para que el personal administrativo gestione las prioridades de las
 * solicitudes pendientes de citacion: listado con filtro, cambio de prioridad e historial.
 */
public class PantallaGestionPrioridades extends JFrame {

    private final PrioridadesService servicio;
    private final Usuario usuario;

    //Reglas activas de CU37 que se pueden aplicar, y sus nombres para el historial
    private List<ReglaPrioridadAplicable> reglas = List.of();
    private final Map<Long, String> nombresReglas = new HashMap<>();

    //Filtro
    private final JComboBox<Prioridad> filtroPrioridad = comboConTodas();
    private final JButton buscar = new JButton("Buscar");

    //Listado
    private final ModeloTablaPrioridades modelo = new ModeloTablaPrioridades();
    private final JTable tabla = new JTable(modelo);

    //Acciones
    private final JButton cambiar = new JButton("Cambiar prioridad");
    private final JButton historial = new JButton("Ver historial");
    private final JLabel estado = new JLabel(" ");

    public PantallaGestionPrioridades(PrioridadesService servicio, Usuario usuario) {

        super("CU28 - Gestionar prioridades");

        if (!disponiblePara(usuario)) {
            throw new IllegalArgumentException(
                    "La pantalla solo esta disponible para el personal administrativo");
        }

        this.servicio = servicio;
        this.usuario = usuario;

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filtros.setBorder(BorderFactory.createTitledBorder("Solicitudes pendientes de citación"));
        filtros.add(new JLabel("Prioridad"));
        filtros.add(filtroPrioridad);
        filtros.add(buscar);
        buscar.addActionListener(e -> cargarSolicitudes(null));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) actualizarBotones();
        });

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(historial);
        botones.add(cambiar);
        cambiar.addActionListener(e -> cambiarSeleccionada());
        historial.addActionListener(e -> verHistorial());

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setBorder(BorderFactory.createEmptyBorder(0, 10, 6, 10));
        inferior.add(estado, BorderLayout.CENTER);
        inferior.add(botones, BorderLayout.EAST);

        JPanel principal = new JPanel(new BorderLayout(8, 8));
        principal.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        principal.add(filtros, BorderLayout.NORTH);
        principal.add(new JScrollPane(tabla), BorderLayout.CENTER);
        principal.add(inferior, BorderLayout.SOUTH);
        add(principal);

        actualizarBotones();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(820, 520);
        setLocationRelativeTo(null);

        //Al abrir se cargan las reglas activas y despues las solicitudes
        cargarReglasYSolicitudes();
    }

    // ---- Carga ----

    private void cargarReglasYSolicitudes() {
        ejecutar("Cargando reglas de prioridad...", () -> servicio.consultarReglasActivas(usuario),
                activas -> {
                    reglas = activas == null ? List.of() : activas;
                    for (ReglaPrioridadAplicable regla : reglas) nombresReglas.put(regla.id(), regla.nombre());
                    cargarSolicitudes(null);
                }, "No se han podido cargar las reglas de prioridad", () -> cargarSolicitudes(null));
    }

    //Consulta el listado; despues selecciona la solicitud indicada si sigue en la tabla
    private void cargarSolicitudes(Long seleccionar) {
        Prioridad filtro = (Prioridad) filtroPrioridad.getSelectedItem();
        ejecutar("Buscando solicitudes...", () -> servicio.consultarPendientes(usuario, filtro), lista -> {
            modelo.setSolicitudes(lista);
            int fila = seleccionar == null ? -1 : modelo.filaDe(seleccionar);
            if (fila >= 0) {
                tabla.setRowSelectionInterval(fila, fila);
            } else {
                tabla.clearSelection();
            }
            actualizarBotones();
            estado.setText(lista.isEmpty()
                    ? "No hay solicitudes pendientes con esa prioridad"
                    : lista.size() + " solicitud(es) pendiente(s)");
        }, "No se han podido cargar las solicitudes", null);
    }

    private SolicitudPrioridad seleccionada() {
        return modelo.getSolicitud(tabla.getSelectedRow());
    }

    private void actualizarBotones() {
        boolean haySeleccion = seleccionada() != null;
        cambiar.setEnabled(haySeleccion);
        historial.setEnabled(haySeleccion);
    }

    // ---- Cambio de prioridad ----

    private void cambiarSeleccionada() {

        SolicitudPrioridad actual = seleccionada();
        if (actual == null) return;

        if (reglas.isEmpty()) {
            error("No hay reglas de prioridad activas. El administrador debe configurarlas (CU37).");
            return;
        }

        JComboBox<Prioridad> nuevaPrioridad = new JComboBox<>(Prioridad.values());
        nuevaPrioridad.setSelectedItem(null);
        JComboBox<ReglaPrioridadAplicable> regla =
                new JComboBox<>(reglas.toArray(new ReglaPrioridadAplicable[0]));
        regla.setSelectedItem(null);
        JTextArea motivo = new JTextArea(3, 25);
        motivo.setLineWrap(true);
        motivo.setWrapStyleWord(true);

        JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 8));
        formulario.add(new JLabel("Prioridad actual"));
        formulario.add(new JLabel(String.valueOf(actual.prioridad())));
        formulario.add(new JLabel("Nueva prioridad *"));
        formulario.add(nuevaPrioridad);
        formulario.add(new JLabel("Regla de prioridad *"));
        formulario.add(regla);
        formulario.add(new JLabel("Motivo *"));
        formulario.add(new JScrollPane(motivo));

        //Se repite el formulario hasta que el formato sea valido o se cancele
        while (true) {
            int opcion = JOptionPane.showConfirmDialog(this, formulario,
                    "Cambiar prioridad de la solicitud " + actual.solicitudId(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) return;

            List<String> errores = ValidadorCambioPrioridad.validar(
                    (Prioridad) nuevaPrioridad.getSelectedItem(), actual.prioridad(),
                    (ReglaPrioridadAplicable) regla.getSelectedItem(), motivo.getText());
            if (errores.isEmpty()) break;
            error(String.join("\n", errores));
        }

        DatosCambioPrioridad datos = new DatosCambioPrioridad(
                (Prioridad) nuevaPrioridad.getSelectedItem(),
                ((ReglaPrioridadAplicable) regla.getSelectedItem()).id(),
                motivo.getText().trim());

        ejecutar("Guardando el cambio de prioridad...",
                () -> servicio.cambiarPrioridad(usuario, actual.solicitudId(), datos),
                actualizada -> {
                    confirmar("Prioridad de la solicitud " + actualizada.solicitudId()
                            + " cambiada de " + actual.prioridad() + " a " + actualizada.prioridad() + ".");
                    cargarSolicitudes(actualizada.solicitudId());
                }, "No se ha cambiado la prioridad", null);
    }

    // ---- Historial ----

    private void verHistorial() {
        SolicitudPrioridad actual = seleccionada();
        if (actual == null) return;

        ejecutar("Consultando el historial...",
                () -> servicio.consultarHistorial(usuario, actual.solicitudId()),
                cambios -> {
                    JTextArea texto = new JTextArea(
                            ModeloTablaPrioridades.textoHistorial(actual.solicitudId(), cambios, nombresReglas),
                            14, 45);
                    texto.setEditable(false);
                    texto.setLineWrap(true);
                    texto.setWrapStyleWord(true);
                    texto.setCaretPosition(0);
                    JOptionPane.showMessageDialog(this, new JScrollPane(texto),
                            "Historial de prioridades", JOptionPane.INFORMATION_MESSAGE);
                    estado.setText(cambios.size() + " cambio(s) de prioridad");
                }, "No se ha podido consultar el historial", null);
    }

    // ---- Utilidades ----

    //Ejecuta una llamada al servicio sin bloquear la ventana y trata el resultado o el error
    private <T> void ejecutar(String mensaje, Supplier<T> llamada, Consumer<T> alTerminar,
            String mensajeFallo, Runnable siFalla) {

        estado.setText(mensaje);
        setControlesActivos(false);

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() {
                return llamada.get();
            }

            @Override
            protected void done() {
                setControlesActivos(true);
                try {
                    alTerminar.accept(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    estado.setText(mensajeFallo);
                } catch (ExecutionException ex) {
                    estado.setText(mensajeFallo);
                    error(mensajeError(ex.getCause()));
                    if (siFalla != null) siFalla.run();
                }
            }
        }.execute();
    }

    private void setControlesActivos(boolean activos) {
        buscar.setEnabled(activos);
        filtroPrioridad.setEnabled(activos);
        if (activos) {
            actualizarBotones();
        } else {
            cambiar.setEnabled(false);
            historial.setEnabled(false);
        }
    }

    private static String mensajeError(Throwable causa) {
        if (causa == null || causa.getMessage() == null) return "Error desconocido";
        return causa.getMessage();
    }

    private void confirmar(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Gestión de prioridades",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error en la gestión de prioridades",
                JOptionPane.ERROR_MESSAGE);
    }

    //Combo de prioridades con una primera opcion vacia que significa "Todas"
    private static JComboBox<Prioridad> comboConTodas() {
        JComboBox<Prioridad> combo = new JComboBox<>();
        combo.addItem(null);
        for (Prioridad prioridad : Prioridad.values()) combo.addItem(prioridad);
        combo.setSelectedItem(null);
        combo.setPreferredSize(new Dimension(150, combo.getPreferredSize().height));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor,
                    int indice, boolean seleccionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, seleccionado, foco);
                if (valor == null) setText("Todas");
                return this;
            }
        });
        return combo;
    }

    //Solo el personal administrativo puede gestionar prioridades (D-016)
    public static boolean disponiblePara(Usuario usuario) {
        return usuario != null && usuario.rol() == Rol.PERSONAL_ADMINISTRATIVO;
    }

    /**
     * Inicia la pantalla. Argumentos opcionales: ROL URL.
     * Sin URL se usa el servicio simulado con datos de ejemplo.
     */
    public static void main(String[] args) {

        Rol rol = args.length > 0 ? Rol.valueOf(args[0]) : Rol.PERSONAL_ADMINISTRATIVO;
        Usuario usuario = new Usuario(System.getProperty("user.name"), rol);

        if (!disponiblePara(usuario)) {
            JOptionPane.showMessageDialog(null,
                    "Solo el personal administrativo puede gestionar prioridades",
                    "Sin permiso", JOptionPane.ERROR_MESSAGE);
            return;
        }

        PrioridadesService servicio = args.length > 1
                ? new PrioridadesServiceHttp(args[1])
                : PrioridadesServiceSimulado.conDatosDeEjemplo();

        SwingUtilities.invokeLater(() ->
                new PantallaGestionPrioridades(servicio, usuario).setVisible(true));
    }
}
