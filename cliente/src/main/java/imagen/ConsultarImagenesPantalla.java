package imagen;

import javax.swing.*;
import java.awt.*;

public class ConsultarImagenesPantalla extends JFrame {
    private JTextField txtIdPrueba;
    private JButton btnBuscar;
    private JLabel lblImagenVisor;

    public ConsultarImagenesPantalla() {
        setTitle("Visor de Imágenes Médicas");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Panel superior: Búsqueda
        JPanel panelNorte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelNorte.add(new JLabel("ID de Prueba:"));
        txtIdPrueba = new JTextField(15);
        panelNorte.add(txtIdPrueba);

        btnBuscar = new JButton("Buscar Imágenes");
        panelNorte.add(btnBuscar);
        add(panelNorte, BorderLayout.NORTH);

        // Panel central: Visor simulado
        lblImagenVisor = new JLabel("Introduce el ID de una prueba para cargar sus imágenes", SwingConstants.CENTER);
        lblImagenVisor.setBorder(BorderFactory.createTitledBorder("Área de visualización radiológica"));
        add(lblImagenVisor, BorderLayout.CENTER);

        // Evento del botón
        btnBuscar.addActionListener(e -> {
            String id = txtIdPrueba.getText().trim();
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor, introduce el ID de la prueba.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Simulación visual hasta que conectemos la API HTTP en la siguiente fase
            lblImagenVisor.setText("Mostrando placa radiográfica de la prueba: " + id + " [Conexión API Pendiente]");
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ConsultarImagenesPantalla().setVisible(true);
        });
    }
}