package informe;

import javax.swing.*;
import java.awt.*;

public class CrearInformePantalla extends JFrame {
    private JTextField txtIdPaciente;
    private JTextArea txtTextoInforme;
    private JButton btnGuardar;

    public CrearInformePantalla() {
        setTitle("Crear Informe de Paciente");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Panel superior: Identificación
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelNorte.add(new JLabel("ID Paciente:"));
        txtIdPaciente = new JTextField(15);
        panelNorte.add(txtIdPaciente);
        add(panelNorte, BorderLayout.NORTH);

        // Panel central: Área de escritura
        txtTextoInforme = new JTextArea();
        txtTextoInforme.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(txtTextoInforme);
        scroll.setBorder(BorderFactory.createTitledBorder("Texto del informe"));
        add(scroll, BorderLayout.CENTER);

        // Panel inferior: Acción
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnGuardar = new JButton("Guardar Informe");
        panelSur.add(btnGuardar);
        add(panelSur, BorderLayout.SOUTH);

        // Evento del botón
        btnGuardar.addActionListener(e -> guardarInforme());
    }

    private void guardarInforme() {
        // Validación de formato exclusiva del cliente (D-013)
        if (txtIdPaciente.getText().trim().isEmpty() || txtTextoInforme.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Rellena todos los campos obligatorios", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Aquí integraremos el cliente HTTP con Gson más adelante
        JOptionPane.showMessageDialog(this, "Validación correcta. Listo para enviar al servidor.");
    }

    // Main temporal para previsualizar el diseño
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CrearInformePantalla().setVisible(true);
        });
    }
}