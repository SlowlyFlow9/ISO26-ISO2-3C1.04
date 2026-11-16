package tipoprueba;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Arranque de prueba: cambia Rol.ADMINISTRADOR por Rol.MEDICO para comprobar que no salen los botones. */
public class AppTiposPrueba {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Tipos de pruebas diagnósticas");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(new PantallaTiposPrueba(new TiposPruebaServicioSimulado(), Rol.ADMINISTRADOR));
            f.setSize(720, 420);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
