package notificacion;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import paciente.Rol;
import paciente.Usuario;

public final class AppNotificacionesPaciente {

    private AppNotificacionesPaciente() {
    }

    public static void main(String[] args) {
        String urlServidor = args.length > 0
                ? args[0]
                : "http://localhost:8080";
        String nombreUsuario = args.length > 1
                ? args[1]
                : System.getProperty("user.name");

        Usuario usuario = new Usuario(
                nombreUsuario,
                Rol.PACIENTE);
        NotificacionesService servicio =
                new NotificacionesServiceHttp(urlServidor);

        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame(
                    "CU05 - Mis notificaciones");
            ventana.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);
            ventana.setContentPane(
                    new PantallaNotificaciones(
                            servicio, usuario, true));
            ventana.setSize(900, 450);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
