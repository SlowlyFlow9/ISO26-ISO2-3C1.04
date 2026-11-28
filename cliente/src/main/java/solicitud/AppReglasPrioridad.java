package solicitud;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import paciente.Rol;
import paciente.Usuario;

public final class AppReglasPrioridad {

    private AppReglasPrioridad() {
    }

    public static void main(String[] args) {
        String urlServidor = args.length > 0
                ? args[0]
                : "http://localhost:8080";
        Usuario administrador = new Usuario(
                System.getProperty("user.name"),
                Rol.ADMINISTRADOR);

        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame(
                    "CU37 - Reglas de prioridad");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setContentPane(new PantallaReglasPrioridad(
                    new ReglasPrioridadServiceHttp(urlServidor),
                    administrador));
            ventana.setSize(850, 500);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
