package cita;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import paciente.Rol;
import paciente.Usuario;

public final class AppPoliticasCita {

    private AppPoliticasCita() {
    }

    public static void main(String[] args) {
        String urlServidor = args.length > 0
                ? args[0]
                : "http://localhost:8080";
        Usuario administrador = new Usuario(
                System.getProperty("user.name"),
                Rol.ADMINISTRADOR);

        SwingUtilities.invokeLater(() -> {
            PoliticaCitaService servicio =
                    new PoliticaCitaServiceHttp(urlServidor);
            JFrame ventana = new JFrame(
                    "CU36 - Politicas de citacion");
            ventana.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);
            ventana.setContentPane(
                    new PantallaPoliticasCita(
                            servicio, administrador));
            ventana.setSize(650, 300);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
