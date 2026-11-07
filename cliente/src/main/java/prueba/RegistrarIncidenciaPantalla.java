package prueba;

import javax.swing.*;
import java.awt.*;

public class RegistrarIncidenciaPantalla extends JFrame {
    private JTextField txtIdPrueba;
    private JTextArea txtDescripcion;
    private JButton btnRegistrar;

    public RegistrarIncidenciaPantalla() {
        setTitle("Registrar Incidencia en Prueba Médica");
        setSize(450, 300);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Panel superior para el ID de Prueba
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelNorte.add(new JLabel("ID de la Prueba:"));
        txtIdPrueba = new JTextField(15);
        panelNorte.add(txtIdPrueba);
        add(panelNorte, BorderLayout.NORTH);

        // Panel central para la descripción de la incidencia
        JPanel panelCentro = new JPanel(new BorderLayout(5, 5));
        panelCentro.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        panelCentro.add(new JLabel("Descripción de la incidencia:"), BorderLayout.NORTH);
        
        txtDescripcion = new JTextArea(5, 30);
        txtDescripcion.setLineWrap(true);
        txtDescripcion.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(txtDescripcion);
        panelCentro.add(scrollPane, BorderLayout.CENTER);
        add(panelCentro, BorderLayout.CENTER);

        // Panel inferior para el botón
        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnRegistrar = new JButton("Registrar Incidencia");
        panelSur.add(btnRegistrar);
        add(panelSur, BorderLayout.SOUTH);

        // Evento del botón
        btnRegistrar.addActionListener(e -> {
            String idPrueba = txtIdPrueba.getText().trim();
            String descripcion = txtDescripcion.getText().trim();

            if (idPrueba.isEmpty() || descripcion.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Simulación visual hasta conectar con el servidor HTTP
            JOptionPane.showMessageDialog(this, "Incidencia registrada correctamente en la prueba " + idPrueba + ".\n[La conexión HTTP se implementará más adelante]", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose(); 
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new RegistrarIncidenciaPantalla().setVisible(true);
        });
    }
}