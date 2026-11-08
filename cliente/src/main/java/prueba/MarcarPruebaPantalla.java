package prueba;

import javax.swing.*;
import java.awt.*;

public class MarcarPruebaPantalla extends JFrame {
    private JTextField txtIdPrueba;
    private JButton btnMarcarRealizada;

    public MarcarPruebaPantalla() {
        setTitle("Marcar Prueba como Realizada");
        setSize(400, 150);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(2, 1, 10, 10));

        // Panel para ID de Prueba
        JPanel panelPrueba = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelPrueba.add(new JLabel("ID de la Prueba:"));
        txtIdPrueba = new JTextField(15);
        panelPrueba.add(txtIdPrueba);
        add(panelPrueba);

        // Panel para el Botón
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnMarcarRealizada = new JButton("Marcar como Realizada");
        panelBoton.add(btnMarcarRealizada);
        add(panelBoton);

        // Evento del botón
        btnMarcarRealizada.addActionListener(e -> {
            String idPrueba = txtIdPrueba.getText().trim();

            if (idPrueba.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El ID de la prueba es obligatorio.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Simulación visual hasta conectar con el servidor
            JOptionPane.showMessageDialog(this, "La prueba " + idPrueba + " ha sido marcada como REALIZADA.\n[La conexión HTTP se implementará más adelante]", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose(); 
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MarcarPruebaPantalla().setVisible(true);
        });
    }
}