package solicitud;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import paciente.Rol;
import paciente.Usuario;

public final class AppListaEspera {

    private AppListaEspera() {
    }

    public static void main(String[] args) {
        String urlServidor = args.length > 0
                ? args[0]
                : "http://localhost:8080";
        Usuario administrativo = new Usuario(
                System.getProperty("user.name"),
                Rol.PERSONAL_ADMINISTRATIVO);

        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame(
                    "CU27 - Gestionar lista de espera");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setContentPane(new PantallaListaEspera(
                    new ListaEsperaServiceHttp(urlServidor),
                    administrativo));
            ventana.setSize(800, 450);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
