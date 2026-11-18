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
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import paciente.Rol;
import paciente.Usuario;
import tipoprueba.TipoPrueba;

/**
 * CU13 - Pantalla para que el medico gestione las solicitudes de pruebas:
 * listado con filtros, detalle, modificacion y anulacion (issue #110).
 */
public class PantallaGestionSolicitudes extends JFrame {

    private final SolicitudesService servicio;
    private final Usuario medico;

    //Nombres de los tipos de prueba por su id
    private final Map<Long, String> nombresTipos = new HashMap<>();

    //Filtros
    private final JTextField filtroPaciente = new JTextField(8);
    private final JComboBox<EstadoSolicitud> filtroEstado = comboConTodos(EstadoSolicitud.values());
    private final JComboBox<Prioridad> filtroPrioridad = comboConTodos(Prioridad.values());
    private final JButton buscar = new JButton("Buscar");
    private final JButton limpiar = new JButton("Limpiar filtros");

    //Listado y detalle
    private final ModeloTablaSolicitudes modelo = new ModeloTablaSolicitudes();
    private final JTable tabla = new JTable(modelo);
    private final JTextArea detalle = new JTextArea(9, 40);

    //Acciones
    private final JButton modificar = new JButton("Modificar");
    private final JButton anular = new JButton("Anular");
    private final JLabel estado = new JLabel(" ");

    public PantallaGestionSolicitudes(SolicitudesService servicio, Usuario medico) {

        super("CU13 - Gestionar solicitudes");

        if (!disponiblePara(medico)) {
            throw new IllegalArgumentException("La pantalla solo esta disponible para medicos");
        }

        this.servicio = servicio;
        this.medico = medico;

        //Filtros
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        filtros.setBorder(BorderFactory.createTitledBorder("Filtros"));
        filtros.add(new JLabel("ID del paciente"));
        filtros.add(filtroPaciente);
        filtros.add(new JLabel("Estado"));
        filtros.add(filtroEstado);
        filtros.add(new JLabel("Prioridad"));
        filtros.add(filtroPrioridad);
        filtros.add(buscar);
        filtros.add(limpiar);

        buscar.addActionListener(e -> cargarSolicitudes(null));
        limpiar.addActionListener(e -> limpiarFiltros());
        filtroPaciente.addActionListener(e -> cargarSolicitudes(null));

        //Tabla
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(false);
        tabla.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) mostrarSeleccionada();
        });
        modelo.setNombreTipo(this::nombreTipo);

        //Detalle
        detalle.setEditable(false);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(true);
        detalle.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel panelDetalle = new JPanel(new BorderLayout());
        panelDetalle.setBorder(BorderFactory.createTitledBorder("Detalle de la solicitud"));
        panelDetalle.add(new JScrollPane(detalle), BorderLayout.CENTER);

        JSplitPane centro = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(tabla), panelDetalle);
        centro.setResizeWeight(0.6);

        //Botones
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(modificar);
        botones.add(anular);
        modificar.addActionListener(e -> modificarSeleccionada());
        anular.addActionListener(e -> anularSeleccionada());

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setBorder(BorderFactory.createEmptyBorder(0, 10, 6, 10));
        inferior.add(estado, BorderLayout.CENTER);
        inferior.add(botones, BorderLayout.EAST);

        JPanel principal = new JPanel(new BorderLayout(8, 8));
        principal.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        principal.add(filtros, BorderLayout.NORTH);
        principal.add(centro, BorderLayout.CENTER);
        principal.add(inferior, BorderLayout.SOUTH);
        add(principal);

        actualizarBotones(null);
        detalle.setText(ModeloTablaSolicitudes.detalle(null, ""));

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);

        //Al abrir se cargan los tipos (para mostrar su nombre) y las solicitudes
        cargarTiposYSolicitudes();
    }

    // ---- Carga del listado ----

    private void cargarTiposYSolicitudes() {
        ejecutar("Cargando solicitudes...", () -> servicio.consultarTiposPrueba(medico), tipos -> {
            if (tipos != null) {
                for (TipoPrueba tipo : tipos) nombresTipos.put(tipo.getId(), tipo.getNombre());
            }
            cargarSolicitudes(null);
        }, "No se han podido cargar los tipos de prueba", () -> cargarSolicitudes(null));
    }

    //Consulta el listado con los filtros; despues selecciona la solicitud indicada si sigue en la tabla
    private void cargarSolicitudes(Long seleccionar) {

        List<String> errores = ValidadorGestionSolicitud.validarFiltroPaciente(filtroPaciente.getText());
        if (!errores.isEmpty()) {
            error(String.join("\n", errores));
            return;
        }

        FiltroSolicitudes filtro = filtroActual();

        ejecutar("Buscando solicitudes...", () -> servicio.consultarSolicitudes(medico, filtro), lista -> {
            modelo.setSolicitudes(lista);
            int fila = seleccionar == null ? -1 : modelo.filaDe(seleccionar);
            if (fila >= 0) {
                tabla.setRowSelectionInterval(fila, fila);
            } else {
                tabla.clearSelection();
            }
            mostrarSeleccionada();
            estado.setText(lista.isEmpty()
                    ? "No hay solicitudes con esos filtros"
                    : lista.size() + " solicitud(es) encontrada(s)");
        }, "No se han podido cargar las solicitudes", null);
    }

    private FiltroSolicitudes filtroActual() {
        String paciente = filtroPaciente.getText().trim();
        return new FiltroSolicitudes(
                paciente.isEmpty() ? null : Long.valueOf(paciente),
                (EstadoSolicitud) filtroEstado.getSelectedItem(),
                (Prioridad) filtroPrioridad.getSelectedItem());
    }

    private void limpiarFiltros() {
        filtroPaciente.setText("");
        filtroEstado.setSelectedItem(null);
        filtroPrioridad.setSelectedItem(null);
        cargarSolicitudes(null);
    }

    private void mostrarSeleccionada() {
        SolicitudPrueba seleccionada = seleccionada();
        detalle.setText(ModeloTablaSolicitudes.detalle(seleccionada,
                seleccionada == null ? "" : nombreTipo(seleccionada.getTipoPruebaId())));
        detalle.setCaretPosition(0);
        actualizarBotones(seleccionada);
    }

    private SolicitudPrueba seleccionada() {
        return modelo.getSolicitud(tabla.getSelectedRow());
    }

    //Modificar y Anular solo se activan con una solicitud pendiente de citacion (D-031)
    private void actualizarBotones(SolicitudPrueba seleccionada) {
        boolean activos = botonesActivos(seleccionada);
        modificar.setEnabled(activos);
        anular.setEnabled(activos);
    }

    // ---- Modificacion ----

    private void modificarSeleccionada() {

        SolicitudPrueba actual = seleccionada();
        if (!botonesActivos(actual)) return;

        JTextArea indicacion = new JTextArea(actual.getIndicacionClinica(), 3, 25);
        JTextArea justificacion = new JTextArea(actual.getJustificacion(), 3, 25);
        indicacion.setLineWrap(true);
        justificacion.setLineWrap(true);
        JComboBox<Prioridad> prioridad = new JComboBox<>(Prioridad.values());
        prioridad.setSelectedItem(actual.getPrioridad());
        JTextField intervalo = new JTextField(texto(actual.getIntervalo()), 6);
        JComboBox<UnidadPeriodicidad> unidad = new JComboBox<>(UnidadPeriodicidad.values());
        unidad.setSelectedItem(actual.getUnidad());
        JTextField repeticiones = new JTextField(texto(actual.getNumeroRepeticiones()), 6);

        JPanel formulario = new JPanel(new GridLayout(0, 2, 8, 8));
        formulario.add(new JLabel("Indicación clínica *"));
        formulario.add(new JScrollPane(indicacion));
        formulario.add(new JLabel("Justificación *"));
        formulario.add(new JScrollPane(justificacion));
        formulario.add(new JLabel("Prioridad *"));
        formulario.add(prioridad);
        if (actual.esSeguimiento()) {
            formulario.add(new JLabel("Intervalo entre pruebas *"));
            formulario.add(intervalo);
            formulario.add(new JLabel("Unidad *"));
            formulario.add(unidad);
            formulario.add(new JLabel("Número de repeticiones *"));
            formulario.add(repeticiones);
        }

        //Se repite el formulario hasta que el formato sea valido o se cancele
        while (true) {
            int opcion = JOptionPane.showConfirmDialog(this, formulario,
                    "Modificar solicitud " + actual.getId(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) return;

            List<String> errores = ValidadorGestionSolicitud.validarModificacion(
                    indicacion.getText(), justificacion.getText(),
                    (Prioridad) prioridad.getSelectedItem(), actual.esSeguimiento(),
                    intervalo.getText(), (UnidadPeriodicidad) unidad.getSelectedItem(),
                    repeticiones.getText());
            if (errores.isEmpty()) break;
            error(String.join("\n", errores));
        }

        DatosModificacionSolicitud datos = new DatosModificacionSolicitud(
                indicacion.getText().trim(),
                justificacion.getText().trim(),
                (Prioridad) prioridad.getSelectedItem(),
                actual.esSeguimiento() ? Integer.valueOf(intervalo.getText().trim()) : null,
                actual.esSeguimiento() ? (UnidadPeriodicidad) unidad.getSelectedItem() : null,
                actual.esSeguimiento() ? Integer.valueOf(repeticiones.getText().trim()) : null);

        ejecutar("Guardando la modificación...",
                () -> servicio.modificarSolicitud(medico, actual.getId(), datos),
                modificada -> {
                    confirmar("Solicitud " + modificada.getId() + " modificada correctamente.");
                    cargarSolicitudes(modificada.getId());
                }, "No se ha modificado la solicitud", null);
    }

    // ---- Anulacion ----

    private void anularSeleccionada() {

        SolicitudPrueba actual = seleccionada();
        if (!botonesActivos(actual)) return;

        JTextArea motivo = new JTextArea(3, 30);
        motivo.setLineWrap(true);
        motivo.setWrapStyleWord(true);
        JPanel panel = new JPanel(new BorderLayout(4, 4));
        panel.add(new JLabel("Motivo de la anulación *"), BorderLayout.NORTH);
        panel.add(new JScrollPane(motivo), BorderLayout.CENTER);

        while (true) {
            int opcion = JOptionPane.showConfirmDialog(this, panel,
                    "Anular solicitud " + actual.getId(),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (opcion != JOptionPane.OK_OPTION) return;

            List<String> errores = ValidadorGestionSolicitud.validarAnulacion(motivo.getText());
            if (errores.isEmpty()) break;
            error(String.join("\n", errores));
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Seguro que quieres anular la solicitud " + actual.getId() + "?\n"
                + "Una solicitud anulada ya no se puede modificar ni citar.",
                "Confirmar anulación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) return;

        String textoMotivo = motivo.getText().trim();

        ejecutar("Anulando la solicitud...",
                () -> servicio.anularSolicitud(medico, actual.getId(), textoMotivo),
                anulada -> {
                    confirmar("Solicitud " + anulada.getId() + " anulada correctamente.");
                    cargarSolicitudes(anulada.getId());
                }, "No se ha anulado la solicitud", null);
    }

    // ---- Utilidades ----

    //Ejecuta una llamada al servicio sin bloquear la ventana y trata el resultado o el error
    private <T> void ejecutar(String mensaje, Supplier<T> llamada,
            java.util.function.Consumer<T> alTerminar, String mensajeFallo, Runnable siFalla) {

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
        limpiar.setEnabled(activos);
        if (activos) {
            actualizarBotones(seleccionada());
        } else {
            modificar.setEnabled(false);
            anular.setEnabled(false);
        }
    }

    private String nombreTipo(long id) {
        return nombresTipos.getOrDefault(id, "ID " + id);
    }

    private static String texto(Integer valor) {
        return valor == null ? "" : valor.toString();
    }

    private static String mensajeError(Throwable causa) {
        if (causa == null || causa.getMessage() == null) return "Error desconocido";
        return causa.getMessage();
    }

    private void confirmar(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Gestión de solicitudes",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Error en la gestión de solicitudes",
                JOptionPane.ERROR_MESSAGE);
    }

    //Combo con una primera opcion vacia que significa "Todos"
    private static <E> JComboBox<E> comboConTodos(E[] valores) {
        JComboBox<E> combo = new JComboBox<>();
        combo.addItem(null);
        for (E valor : valores) combo.addItem(valor);
        combo.setSelectedItem(null);
        combo.setPreferredSize(new Dimension(170, combo.getPreferredSize().height));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor,
                    int indice, boolean seleccionado, boolean foco) {
                super.getListCellRendererComponent(lista, valor, indice, seleccionado, foco);
                if (valor == null) setText("Todos");
                return this;
            }
        });
        return combo;
    }

    //Solo los medicos pueden utilizar esta pantalla
    public static boolean disponiblePara(Usuario usuario) {
        return usuario != null && usuario.rol() == Rol.MEDICO;
    }

    //Modificar y Anular solo estan disponibles para una solicitud pendiente de citacion
    public static boolean botonesActivos(SolicitudPrueba solicitud) {
        return solicitud != null && solicitud.sePuedeGestionar();
    }

    /**
     * Inicia la pantalla. Argumentos opcionales: ROL URL.
     * Sin URL se usa el servicio simulado con datos de ejemplo.
     */
    public static void main(String[] args) {

        Rol rol = args.length > 0 ? Rol.valueOf(args[0]) : Rol.MEDICO;
        Usuario usuario = new Usuario(System.getProperty("user.name"), rol);

        if (!disponiblePara(usuario)) {
            JOptionPane.showMessageDialog(null,
                    "Solo el rol Medico puede gestionar solicitudes",
                    "Sin permiso", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SolicitudesService servicio = args.length > 1
                ? new SolicitudesServiceHttp(args[1])
                : SolicitudesServiceSimulado.conDatosDeEjemplo();

        SwingUtilities.invokeLater(() ->
                new PantallaGestionSolicitudes(servicio, usuario).setVisible(true));
    }
}
