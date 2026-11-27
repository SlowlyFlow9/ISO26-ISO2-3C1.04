package imagen;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import paciente.Rol;
import paciente.Usuario;

public final class AppCrearImagenes {

    private AppCrearImagenes() {
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

        ImagenesServiceHttp servicio =
                new ImagenesServiceHttp(urlServidor);

        SwingUtilities.invokeLater(() -> {

            JFrame ventana = new JFrame(
                    "CU17 y CU18 - Gestión de imágenes");

            ventana.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);

            JTabbedPane pestanas = new JTabbedPane();

            pestanas.addTab(
                    "Crear imágenes",
                    new PantallaCrearImagenes(
                            servicio, usuario));

            pestanas.addTab(
                    "Consultar imágenes",
                    new PantallaConsultarImagenes(
                            servicio, usuario));

            ventana.setContentPane(pestanas);

            ventana.setSize(1050, 650);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}