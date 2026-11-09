package prueba;

import javax.swing.*;
import java.awt.*;

public class ConsultarEstadisticasPantalla extends JFrame {
    private JButton btnRefrescar;

    public ConsultarEstadisticasPantalla() {
        setTitle("Estadísticas de Pruebas Médicas");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Panel central con los contadores (cifras simuladas hasta conectar con el servidor)
        JPanel panelCentro = new JPanel(new GridLayout(2, 2, 10, 10));
        panelCentro.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        panelCentro.add(new JLabel("PENDIENTES:"));
        panelCentro.add(new JLabel("0", SwingConstants.CENTER));
        
        panelCentro.add(new JLabel("REALIZADAS:"));
        panelCentro.add(new JLabel("0", SwingConstants.CENTER));

        add(panelCentro, BorderLayout.CENTER);

        // Botón para refrescar los datos
        JPanel panelSur = new JPanel();
        btnRefrescar = new JButton("Refrescar Datos");
        panelSur.add(btnRefrescar);
        add(panelSur, BorderLayout.SOUTH);

        // Evento del botón
        btnRefrescar.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Consultando al servidor...\n[La integración HTTP/Gson se implementará en la siguiente fase]", "Información", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ConsultarEstadisticasPantalla().setVisible(true);
        });
    }
}