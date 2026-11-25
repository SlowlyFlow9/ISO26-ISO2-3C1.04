package agenda;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import paciente.Rol;
import paciente.Usuario;

/** Arranque de la pantalla CU14 para pruebas locales. */
public final class AppAgendasRealizacion {

    private AppAgendasRealizacion() {
    }

    public static void main(String[] args) {
        String urlServidor = args.length > 0
                ? args[0]
                : "http://localhost:8080";
        long medicoId = args.length > 1
                ? Long.parseLong(args[1])
                : 1L;

        if (medicoId <= 0) {
            throw new IllegalArgumentException(
                    "El identificador del médico debe ser positivo");
        }

        Usuario usuario = new Usuario(
                System.getProperty("user.name"),
                Rol.MEDICO,
                medicoId);

        SwingUtilities.invokeLater(() -> {
            AgendasRealizacionService servicio =
                    new AgendasServiceHttp(urlServidor);

            JFrame ventana = new JFrame(
                    "CU14 - Agendas de realización");
            ventana.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);
            ventana.setContentPane(
                    new PantallaAgendasRealizacion(
                            servicio, usuario));
            ventana.setSize(750, 450);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
