package tipoprueba;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
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
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

/** Pantalla de gestión de tipos de prueba. Sin lógica de negocio: delega todo en el servicio. */
public class PantallaTiposPrueba extends JPanel {
    private static final long serialVersionUID = 1L;

    private final TiposPruebaService servicio;
    private final boolean esAdministrador;

    private final JTextField txtBuscar = new JTextField(20);
    private final JComboBox<FiltroTipoPrueba.Estado> cmbEstado =
            new JComboBox<>(FiltroTipoPrueba.Estado.values());
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[] { "Nombre", "Duración estimada (min)", "Estado" }, 0) {
        private static final long serialVersionUID = 1L;
        @Override public boolean isCellEditable(int fila, int col) { return false; }
    };
    private final JTable tabla = new JTable(modelo);
    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnModificar = new JButton("Modificar");
    private final JButton btnDesactivar = new JButton("Desactivar");
    private final JLabel lblMensaje = new JLabel(" ");

    private List<TipoPrueba> mostrados = new ArrayList<>();

    public PantallaTiposPrueba(TiposPruebaService servicio, Rol rol) {
        this.servicio = servicio;
        this.esAdministrador = rol == Rol.ADMINISTRADOR;
        construir();
        cargar();
    }

    private void construir() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtros.add(new JLabel("Buscar por nombre:"));
        filtros.add(txtBuscar);
        filtros.add(new JLabel("Estado:"));
        filtros.add(cmbEstado);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(e -> actualizarBotones());

        // Los botones de escritura solo se muestran al Administrador.
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnNuevo.setVisible(esAdministrador);
        btnModificar.setVisible(esAdministrador);
        btnDesactivar.setVisible(esAdministrador);
        botones.add(btnNuevo);
        botones.add(btnModificar);
        botones.add(btnDesactivar);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(botones, BorderLayout.NORTH);
        sur.add(lblMensaje, BorderLayout.SOUTH);

        add(filtros, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);

        txtBuscar.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { cargar(); }
            @Override public void removeUpdate(DocumentEvent e) { cargar(); }
            @Override public void changedUpdate(DocumentEvent e) { cargar(); }
        });
        cmbEstado.addActionListener(e -> cargar());
        btnNuevo.addActionListener(e -> nuevo());
        btnModificar.addActionListener(e -> modificar());
        btnDesactivar.addActionListener(e -> desactivar());
        actualizarBotones();
    }

    /** Recarga el listado aplicando búsqueda y filtro. */
    public void cargar() {
        try {
            FiltroTipoPrueba filtro = new FiltroTipoPrueba(txtBuscar.getText(),
                    (FiltroTipoPrueba.Estado) cmbEstado.getSelectedItem());
            mostrados = new ArrayList<>(servicio.listar(filtro));
            modelo.setRowCount(0);
            for (TipoPrueba t : mostrados) {
                modelo.addRow(new Object[] { t.getNombre(), t.getDuracionEstimada(),
                        t.isActivo() ? "Activo" : "Inactivo" });
            }
        } catch (ServicioException ex) {
            mostrarError(ex.getMessage());
        }
        actualizarBotones();
    }

    private TipoPrueba seleccionado() {
        int fila = tabla.getSelectedRow();
        return fila >= 0 && fila < mostrados.size() ? mostrados.get(fila) : null;
    }

    private void actualizarBotones() {
        TipoPrueba t = seleccionado();
        btnModificar.setEnabled(t != null);
        btnDesactivar.setEnabled(t != null && t.isActivo());
    }

    private void nuevo() {
        boolean ok = new FormularioTipoPruebaDialog(SwingUtilities.getWindowAncestor(this), servicio, null).mostrar();
        if (ok) {
            cargar();
            mostrarExito(Mensajes.EXITO_CREAR);
        }
    }

    private void modificar() {
        TipoPrueba t = seleccionado();
        if (t == null) {
            return;
        }
        boolean ok = new FormularioTipoPruebaDialog(SwingUtilities.getWindowAncestor(this), servicio, t).mostrar();
        if (ok) {
            cargar();
            mostrarExito(Mensajes.EXITO_MODIFICAR);
        }
    }

    private void desactivar() {
        TipoPrueba t = seleccionado();
        if (t == null) {
            return;
        }
        try {
            int pendientes = servicio.contarPruebasPendientes(t.getId());
            String texto = (pendientes > 0 ? String.format(Mensajes.AVISO_PENDIENTES, pendientes) : "")
                    + String.format(Mensajes.CONFIRMAR_DESACTIVAR, t.getNombre());
            int opcion = JOptionPane.showConfirmDialog(this, texto, "Confirmar desactivación",
                    JOptionPane.YES_NO_OPTION,
                    pendientes > 0 ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE);
            if (opcion != JOptionPane.YES_OPTION) {
                return;
            }
            servicio.desactivar(t.getId());
            cargar();
            mostrarExito(Mensajes.EXITO_DESACTIVAR);
        } catch (ServicioException ex) {
            mostrarError(ex.getMessage());
        }
    }

    private void mostrarExito(String texto) {
        lblMensaje.setForeground(new Color(0, 120, 0));
        lblMensaje.setText(texto);
    }

    private void mostrarError(String texto) {
        lblMensaje.setForeground(new Color(180, 0, 0));
        lblMensaje.setText(Mensajes.ERROR_GENERICO + texto);
    }
}
