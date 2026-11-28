package agenda;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import cita.CitasIncidenciasService;
import cita.CitasIncidenciasServiceHttp;
import cita.CitasRealizacionService;
import cita.CitasRealizacionServiceHttp;
import cita.PantallaIncidenciasCita;
import cita.PantallaRealizarPrueba;
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
            CitasRealizacionService servicioCitas =
                    new CitasRealizacionServiceHttp(urlServidor);
            CitasIncidenciasService servicioIncidencias =
                    new CitasIncidenciasServiceHttp(urlServidor);

            JTabbedPane pestanas = new JTabbedPane();
            pestanas.addTab(
                    "Agendas (CU14)",
                    new PantallaAgendasRealizacion(servicio, usuario));
            pestanas.addTab(
                    "Pruebas (CU19)",
                    new PantallaRealizarPrueba(
                            servicioCitas, usuario));
            pestanas.addTab(
                    "Incidencias (CU20)",
                    new PantallaIncidenciasCita(
                            servicioIncidencias, usuario));

            JFrame ventana = new JFrame(
                    "Gestión médica - agendas, pruebas e incidencias");
            ventana.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE);
            ventana.setContentPane(pestanas);
            ventana.setSize(750, 450);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }
}
