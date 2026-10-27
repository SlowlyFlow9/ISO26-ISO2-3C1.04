package paciente;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/** CU22: formulario de alta de paciente. Solo valida formato (D-013); lo demás lo decide el servicio. */
public class PantallaAltaPaciente extends JFrame {
    private final PacientesService servicio;
    private final JTextField nombre = new JTextField(20);
    private final JTextField apellidos = new JTextField(20);
    private final JTextField dni = new JTextField(20);
    private final JComboBox<String> dia = combo("Día", 1, 31);
    private final JComboBox<String> mes = combo("Mes", 1, 12);
    private final JComboBox<String> anio = combo("Año", LocalDate.now().getYear(), 1900);
    private final JTextField telefono = new JTextField(20);
    private final JTextField email = new JTextField(20);
    private final JTextField direccion = new JTextField(20);
    private final JTextField tarjeta = new JTextField(20);

    public PantallaAltaPaciente(PacientesService servicio) {
        super("Alta de paciente");
        this.servicio = servicio;
        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        campo(formulario, "Nombre *", nombre);
        campo(formulario, "Apellidos *", apellidos);
        campo(formulario, "DNI/NIE *", dni);
        JPanel fecha = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        fecha.add(dia);
        fecha.add(mes);
        fecha.add(anio);
        formulario.add(new JLabel("Fecha de nacimiento *"));
        formulario.add(fecha);
        campo(formulario, "Teléfono *", telefono);
        campo(formulario, "Email", email);
        campo(formulario, "Dirección", direccion);
        campo(formulario, "Tarjeta sanitaria", tarjeta);
        JButton alta = new JButton("Dar de alta");
        alta.addActionListener(e -> darDeAlta());
        formulario.add(new JLabel());
        formulario.add(alta);
        add(formulario);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    /** Desplegable con un primer elemento "sin elegir" y los números de desde a hasta (en orden descendente si desde > hasta). */
    private static JComboBox<String> combo(String titulo, int desde, int hasta) {
        JComboBox<String> combo = new JComboBox<>();
        combo.addItem(titulo);
        int paso = desde <= hasta ? 1 : -1;
        for (int i = desde; i != hasta + paso; i += paso) combo.addItem(String.valueOf(i));
        return combo;
    }

    private static void campo(JPanel panel, String etiqueta, JTextField texto) {
        panel.add(new JLabel(etiqueta));
        panel.add(texto);
    }

    private void darDeAlta() {
        Paciente p = new Paciente();
        p.setNombre(nombre.getText());
        p.setApellidos(apellidos.getText());
        p.setDni(dni.getText());
        p.setTelefono(telefono.getText());
        p.setEmail(email.getText());
        p.setDireccion(direccion.getText());
        p.setNumeroTarjetaSanitaria(tarjeta.getText());
        if (dia.getSelectedIndex() > 0 && mes.getSelectedIndex() > 0 && anio.getSelectedIndex() > 0) {
            try {
                p.setFechaNacimiento(LocalDate.of(Integer.parseInt((String) anio.getSelectedItem()),
                        mes.getSelectedIndex(), dia.getSelectedIndex()));
            } catch (DateTimeException ex) {
                error("La fecha de nacimiento no existe");
                return;
            }
        }
        List<String> errores = ValidadorPaciente.validar(p);
        if (!errores.isEmpty()) {
            error(String.join("\n", errores));
            return;
        }
        try {
            Paciente registrado = servicio.crear(p);
            JOptionPane.showMessageDialog(this, "Paciente dado de alta con id " + registrado.getId());
            limpiar();
        } catch (DatosInvalidosException ex) {
            error(String.join("\n", ex.getErrores()));
        } catch (PacienteDuplicadoException ex) {
            error(ex.getMessage());
        }
    }

    private void error(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "No se puede dar de alta", JOptionPane.ERROR_MESSAGE);
    }

    private void limpiar() {
        for (JTextField f : List.of(nombre, apellidos, dni, telefono, email, direccion, tarjeta)) f.setText("");
        dia.setSelectedIndex(0);
        mes.setSelectedIndex(0);
        anio.setSelectedIndex(0);
    }

    public static void main(String[] args) {
        // ponytail: servicio simulado, sustituir por la implementación real cuando exista el servidor (D-022)
        SwingUtilities.invokeLater(() -> new PantallaAltaPaciente(new PacientesServiceSimulado()).setVisible(true));
    }
}
