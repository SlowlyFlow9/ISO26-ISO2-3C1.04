package imagen;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import paciente.Rol;
import paciente.Usuario;

public final class AppConsultarImagenes {

    private AppConsultarImagenes() {
    }

    public static void main(String[] args) {

        String urlServidor = args.length > 0
                ? args[0]
                : "http://localhost:8080";

        String nombreUsuario = args.length > 1
                ? args[1]
                : System.getProperty("user.name");

        long medicoId = args.length > 2
                ? Long.parseLong(args[2])
                : 1L;

        Usuario usuario = new Usuario(
                nombreUsuario,
                Rol.MEDICO,
                medicoId);

        ImagenesService servicio =
                new ImagenesServiceHttp(urlServidor);

        SwingUtilities.invokeLater(() -> {

            JFrame ventana = new JFrame(
                    "CU17 - Consultar imágenes de pruebas");

            ventana.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);

            ventana.setContentPane(
                    new PantallaConsultarImagenes(
                            servicio, usuario));

            ventana.setSize(1000, 600);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}