package cita;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

/** Interfaz médica para CU19. */
public class PantallaRealizarPrueba extends JPanel {

    private static final long serialVersionUID = 1L;

    private final CitasRealizacionService servicio;
    private final Usuario usuario;
    private final JTextField campoCitaId = new JTextField();
    private final JLabel mensaje = new JLabel(" ");

    public PantallaRealizarPrueba(
            CitasRealizacionService servicio,
            Usuario usuario) {
        if (servicio == null) {
            throw new IllegalArgumentException(
                    "El servicio es obligatorio");
        }
        if (!disponiblePara(usuario)) {
            throw new IllegalArgumentException(
                    "La pantalla requiere un médico identificado");
        }
        this.servicio = servicio;
        this.usuario = usuario;
        construir();
    }

    public static boolean disponiblePara(Usuario usuario) {
        return usuario != null
                && usuario.rol() == Rol.MEDICO
                && usuario.medicoId() != null
                && usuario.medicoId() > 0;
    }

    private void construir() {
        setLayout(new BorderLayout(8, 8));

        campoCitaId.setName("campoCitaId");
        mensaje.setName("mensaje");

        JPanel formulario = new JPanel(new GridLayout(0, 1, 4, 4));
        formulario.add(new JLabel("ID de la cita"));
        formulario.add(campoCitaId);

        JButton marcarRealizada =
                new JButton("Marcar prueba como realizada");
        marcarRealizada.setName("botonMarcarRealizada");
        marcarRealizada.addActionListener(evento -> marcarRealizada());

        JPanel acciones = new JPanel();
        acciones.add(marcarRealizada);

        add(formulario, BorderLayout.NORTH);
        add(acciones, BorderLayout.CENTER);
        add(mensaje, BorderLayout.SOUTH);
    }

    private void marcarRealizada() {
        try {
            Cita cita = servicio.marcarRealizada(
                    usuario, leerCitaId());
            mensaje.setText(
                    "La cita " + cita.getId()
                            + " se ha marcado como realizada");
        } catch (SinPermisoException
                | CitaNoEncontradaException
                | CitaNoRealizableException
                | IllegalArgumentException
                | ServicioNoDisponibleException e) {
            mensaje.setText("Error: "
                    + (e.getMessage() == null
                            ? "operación fallida"
                            : e.getMessage()));
        }
    }

    private long leerCitaId() {
        try {
            long id = Long.parseLong(campoCitaId.getText().trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Introduce un identificador de cita positivo");
        }
    }
}
