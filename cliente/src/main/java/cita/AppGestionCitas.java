package cita;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import paciente.Paciente;
import paciente.PacientesServiceSimulado;
import paciente.Rol;
import paciente.Usuario;
import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;

/**
 * CU25: lanzador independiente de la pantalla de gestión de citas.
 * Usa servicios simulados con datos de demostración (todavía no hay rutas
 * HTTP para CU25, D-032).
 */
public final class AppGestionCitas {

    private AppGestionCitas() {
    }

    public static void main(String[] args) throws Exception {

        Usuario usuario = new Usuario(
                System.getProperty("user.name"),
                Rol.PERSONAL_ADMINISTRATIVO);

        PacientesServiceSimulado pacientes =
                new PacientesServiceSimulado();

        Paciente ana = crearPaciente(
                pacientes, usuario, "Ana", "García", "12345678Z");
        Paciente luis = crearPaciente(
                pacientes, usuario, "Luis", "Martín", "87654321X");

        CitasAdministracionServiceSimulado servicio =
                new CitasAdministracionServiceSimulado(pacientes);

        LocalDate manana = LocalDate.now().plusDays(1);

        servicio.agregarSolicitud(solicitud(1, ana.getId(),
                Prioridad.ORDINARIA, "Dolor lumbar"));
        servicio.agregarSolicitud(solicitud(2, ana.getId(),
                Prioridad.URGENTE, "Control de nódulo"));
        servicio.agregarSolicitud(solicitud(3, luis.getId(),
                Prioridad.PREFERENTE, "Revisión de rodilla"));

        servicio.agregarHueco(1, manana,
                LocalTime.of(9, 0), LocalTime.of(9, 30), 7, false, true);
        servicio.agregarHueco(2, manana,
                LocalTime.of(9, 30), LocalTime.of(10, 0), 7, false, true);
        servicio.agregarHueco(3, manana,
                LocalTime.of(10, 0), LocalTime.of(10, 30), 7, true, true);
        servicio.agregarHueco(4, manana.plusDays(1),
                LocalTime.of(11, 0), LocalTime.of(11, 30), 8, false, true);

        SwingUtilities.invokeLater(() -> {
            JFrame ventana = new JFrame(
                    "CU25 - Gestión de las citas de un paciente");
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setContentPane(new PantallaGestionCitas(
                    servicio, pacientes, usuario));
            ventana.setSize(850, 600);
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
        });
    }

    private static Paciente crearPaciente(
            PacientesServiceSimulado servicio,
            Usuario usuario,
            String nombre,
            String apellidos,
            String dni) throws Exception {

        Paciente datos = new Paciente();
        datos.setNombre(nombre);
        datos.setApellidos(apellidos);
        datos.setDni(dni);
        datos.setFechaNacimiento(LocalDate.of(1980, 5, 20));
        datos.setTelefono("600123456");

        return servicio.crear(usuario, datos);
    }

    private static SolicitudPrueba solicitud(
            long id,
            long pacienteId,
            Prioridad prioridad,
            String indicacion) {

        return new SolicitudPrueba(
                id, pacienteId, 5, 3,
                indicacion, "Control", prioridad,
                LocalDateTime.now(),
                EstadoSolicitud.PENDIENTE_CITACION);
    }
}
