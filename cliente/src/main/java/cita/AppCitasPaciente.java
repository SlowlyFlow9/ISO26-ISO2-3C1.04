package cita;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU01 y CU02 - Arranque de la pantalla de citas.
 */

public final class AppCitasPaciente {

    private AppCitasPaciente() {
    }

    public static void main(String[] args) {

        String urlServidor = args.length > 0
                ? args[0]
                : "http://localhost:8080";

        String nombreUsuario = args.length > 1
                ? args[1]
                : System.getProperty("user.name");

        //Identificacion provisional del proyecto
        Usuario usuario = new Usuario(
                nombreUsuario,
                Rol.PACIENTE);

        //dependemos del contrato, no de una clase concreta de la interfaz grafica
        CitasService servicio =
                new CitasServiceHttp(urlServidor);

        SwingUtilities.invokeLater(() -> {

            JFrame ventana = new JFrame(
                    "CU01 y CU02 - Mis citas");

            ventana.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);

            ventana.setContentPane(
                    new PantallaCitasPaciente(
                            servicio, usuario, true));

            ventana.setSize(850, 450);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
