package informe;

import javax.swing.*;
import java.awt.*;

public class ConsultarInformesPantalla extends JFrame {
    private JTextField txtIdPaciente;
    private JButton btnConsultar;
    private JTextArea txtResultados;

    public ConsultarInformesPantalla() {
        setTitle("Consultar Informes de Paciente");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Panel superior: Búsqueda
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelNorte.add(new JLabel("ID Paciente:"));
        txtIdPaciente = new JTextField(15);
        panelNorte.add(txtIdPaciente);
        
        btnConsultar = new JButton("Consultar");
        panelNorte.add(btnConsultar);
        add(panelNorte, BorderLayout.NORTH);

        // Panel central: Resultados
        txtResultados = new JTextArea();
        txtResultados.setEditable(false);
        txtResultados.setLineWrap(true);
        txtResultados.setWrapStyleWord(true);
        
        JScrollPane scroll = new JScrollPane(txtResultados);
        scroll.setBorder(BorderFactory.createTitledBorder("Historial de informes"));
        add(scroll, BorderLayout.CENTER);

        // Evento del botón (Simulado hasta que hagamos la conexión HTTP)
        btnConsultar.addActionListener(e -> {
            String id = txtIdPaciente.getText().trim();
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor, introduce el ID del paciente.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Aquí irá la llamada HTTP al servidor más adelante
            txtResultados.setText("Buscando informes para el paciente: " + id + "...\n\n[El sistema de conexión al servidor se integrará en la fase de API HTTP]");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ConsultarInformesPantalla().setVisible(true);
        });
    }
}
