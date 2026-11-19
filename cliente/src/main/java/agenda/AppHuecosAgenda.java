package agenda;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU29 - Arranque de la pantalla de huecos.
 *
 * Uso: AppHuecosAgenda [URL del servidor]
 */

public final class AppHuecosAgenda {

    private AppHuecosAgenda() {
    }

    public static void main(String[] args) {

        String urlServidor = args.length > 0
                ? args[0]
                : "http://localhost:8080";

        // Usuario provisional para las pruebas locales.
        Usuario usuario = new Usuario(
                System.getProperty("user.name"),
                Rol.PERSONAL_ADMINISTRATIVO);

        SwingUtilities.invokeLater(() -> {

            HuecosAgendaService servicio =
                    new HuecosAgendaServiceHttp(urlServidor);

            JFrame ventana = new JFrame(
                    "CU29 - Gestión de huecos de agenda");

            ventana.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);

            ventana.setContentPane(
                    new PantallaHuecosAgenda(
                            servicio, usuario));

            ventana.setSize(750, 450);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });

    }

}
