package tipoprueba;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/** Formulario de alta y edición. Si el servicio falla, se queda abierto con los datos escritos. */
public class FormularioTipoPruebaDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final TiposPruebaService servicio;
    private final TipoPrueba existente; // null = alta
    private final ValidadorTipoPrueba validador = new ValidadorTipoPrueba();

    private final JTextField txtNombre = new JTextField(25);
    private final JTextArea txtDescripcion = new JTextArea(3, 25);
    private final JTextField txtDuracion = new JTextField(6);
    private final JCheckBox chkPreparacion = new JCheckBox("Requiere preparación");
    private final JTextArea txtInstrucciones = new JTextArea(3, 25);
    private final JLabel lblError = new JLabel(" ");

    private boolean guardado = false;

    public FormularioTipoPruebaDialog(Window owner, TiposPruebaService servicio, TipoPrueba existente) {
        super(owner, existente == null ? "Nuevo tipo de prueba" : "Modificar tipo de prueba",
                ModalityType.APPLICATION_MODAL);
        this.servicio = servicio;
        this.existente = existente;
        construir();
        if (existente != null) {
            rellenar(existente);
        }
        actualizarInstrucciones();
        pack();
        setLocationRelativeTo(owner);
    }

    private void construir() {
        JPanel campos = new JPanel(new GridBagLayout());
        campos.setBorder(new EmptyBorder(12, 12, 4, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        fila(campos, c, 0, "Nombre *", txtNombre);
        fila(campos, c, 1, "Descripción", new JScrollPane(txtDescripcion));
        fila(campos, c, 2, "Duración estimada (min) *", txtDuracion);
        c.gridx = 1; c.gridy = 3;
        campos.add(chkPreparacion, c);
        fila(campos, c, 4, "Instrucciones al paciente", new JScrollPane(txtInstrucciones));

        txtDescripcion.setLineWrap(true);
        txtInstrucciones.setLineWrap(true);
        chkPreparacion.addActionListener(e -> actualizarInstrucciones());

        lblError.setForeground(new Color(180, 0, 0));
        lblError.setBorder(new EmptyBorder(0, 12, 0, 12));

        JButton btnGuardar = new JButton("Guardar");
        JButton btnCancelar = new JButton("Cancelar");
        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> dispose());
        getRootPane().setDefaultButton(btnGuardar);

        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(btnCancelar);
        botones.add(btnGuardar);

        JPanel sur = new JPanel(new BorderLayout());
        sur.add(lblError, BorderLayout.NORTH);
        sur.add(botones, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(campos, BorderLayout.CENTER);
        add(sur, BorderLayout.SOUTH);
    }

    private void fila(JPanel panel, GridBagConstraints c, int y, String etiqueta, java.awt.Component campo) {
        c.gridx = 0; c.gridy = y; c.weightx = 0;
        panel.add(new JLabel(etiqueta), c);
        c.gridx = 1; c.weightx = 1;
        panel.add(campo, c);
    }

    private void rellenar(TipoPrueba t) {
        txtNombre.setText(t.getNombre());
        txtDescripcion.setText(t.getDescripcion());
        txtDuracion.setText(String.valueOf(t.getDuracionEstimada()));
        chkPreparacion.setSelected(t.isRequierePreparacion());
        txtInstrucciones.setText(t.getInstruccionesPaciente());
    }

    private void actualizarInstrucciones() {
        txtInstrucciones.setEnabled(chkPreparacion.isSelected());
    }

    private void guardar() {
        ValidadorTipoPrueba.Resultado r = validador.validar(txtNombre.getText(),
                txtDescripcion.getText(), txtDuracion.getText(),
                chkPreparacion.isSelected(), txtInstrucciones.getText());
        if (!r.esValido()) {
            mostrarError(String.join(" ", r.getErrores()));
            return;
        }
        try {
            if (existente == null) {
                servicio.crear(r.getDatos());
            } else {
                servicio.modificar(existente.getId(), r.getDatos());
            }
            guardado = true;
            dispose();
        } catch (ServicioException ex) {
            // Se muestra el error y NO se cierra: el usuario conserva lo que había escrito.
            mostrarError(ex.getMessage());
        }
    }

    private void mostrarError(String texto) {
        lblError.setText("<html>" + texto.replace("<", "&lt;") + "</html>");
        pack();
    }

    /** Muestra el diálogo (modal) y devuelve true si se guardó correctamente. */
    public boolean mostrar() {
        setVisible(true);
        return guardado;
    }
}
