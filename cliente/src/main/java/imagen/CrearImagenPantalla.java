package imagen;

import javax.swing.*;
import java.awt.*;

public class CrearImagenPantalla extends JFrame {
    private JTextField txtIdPrueba;
    private JTextField txtRutaImagen;
    private JButton btnGuardar;

    public CrearImagenPantalla() {
        setTitle("Añadir Nueva Imagen Médica");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(3, 1, 10, 10));

        // Panel para ID de Prueba
        JPanel panelPrueba = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelPrueba.add(new JLabel("ID de la Prueba:"));
        txtIdPrueba = new JTextField(15);
        panelPrueba.add(txtIdPrueba);
        add(panelPrueba);

        // Panel para Ruta de Imagen
        JPanel panelRuta = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelRuta.add(new JLabel("Ruta de la Imagen:"));
        txtRutaImagen = new JTextField(15);
        panelRuta.add(txtRutaImagen);
        add(panelRuta);

        // Panel para el Botón
        JPanel panelBoton = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnGuardar = new JButton("Guardar Imagen");
        panelBoton.add(btnGuardar);
        add(panelBoton);

        // Evento del botón
        btnGuardar.addActionListener(e -> {
            String idPrueba = txtIdPrueba.getText().trim();
            String ruta = txtRutaImagen.getText().trim();

            if (idPrueba.isEmpty() || ruta.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Simulación de guardado hasta conectar con el servidor
            JOptionPane.showMessageDialog(this, "Imagen registrada correctamente para la prueba: " + idPrueba + "\n[La conexión HTTP se implementará más adelante]", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose(); 
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CrearImagenPantalla().setVisible(true);
        });
    }
}