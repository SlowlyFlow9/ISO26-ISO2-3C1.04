package cita;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU06 - Aplicacion de consulta del historial
 */
public final class AppHistorialPruebas {

    private AppHistorialPruebas() {
    }

    public static void main(String[] args) {

        String urlServidor = args.length > 0
                ? args[0]
                : "http://localhost:8080";

        String nombreUsuario = args.length > 1
                ? args[1]
                : System.getProperty("user.name");

        Usuario usuario = new Usuario(
                nombreUsuario, Rol.PACIENTE);

        HistorialPruebasService servicio =
                new HistorialPruebasServiceHttp(urlServidor);

        SwingUtilities.invokeLater(() -> {

            JFrame ventana = new JFrame(
                    "CU06 - Mi historial de pruebas");

            ventana.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);

            ventana.setContentPane(
                    new PantallaHistorialPruebas(
                            servicio, usuario, true));

            ventana.setSize(850, 450);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
