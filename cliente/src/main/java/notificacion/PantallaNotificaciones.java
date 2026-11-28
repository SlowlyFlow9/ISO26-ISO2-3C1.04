package notificacion;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import paciente.Rol;
import paciente.Usuario;

public class PantallaNotificaciones extends JPanel {

    private final NotificacionesService servicio;
    private final Usuario usuario;
    private final boolean asincrono;
    private int versionListado;

    private final DefaultTableModel modelo =
            new DefaultTableModel(
                    new Object[] {
                            "Título",
                            "Mensaje",
                            "Fecha y hora",
                            "Estado"
                    }, 0) {
                @Override
                public boolean isCellEditable(
                        int fila, int columna) {
                    return false;
                }
            };

    private final JTable tabla = new JTable(modelo);
    private final JLabel mensaje = new JLabel();

    public PantallaNotificaciones(
            NotificacionesService servicio,
            Usuario usuario) {
        this(servicio, usuario, false);
    }

    public PantallaNotificaciones(
            NotificacionesService servicio,
            Usuario usuario,
            boolean asincrono) {
        if (servicio == null) {
            throw new IllegalArgumentException(
                    "El servicio de notificaciones es obligatorio");
        }

        this.servicio = servicio;
        this.usuario = usuario;
        this.asincrono = asincrono;

        construir();
        actualizar();
    }

    public static boolean disponiblePara(Usuario usuario) {
        return usuario != null
                && usuario.rol() == Rol.PACIENTE;
    }

    private void construir() {
        setLayout(new BorderLayout(10, 10));
        add(new JLabel("Mis notificaciones"), BorderLayout.NORTH);

        tabla.setName("tablaNotificaciones");
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout());
        mensaje.setName("mensajeNotificaciones");
        inferior.add(mensaje, BorderLayout.CENTER);

        JButton actualizar =
                new JButton("Actualizar");
        actualizar.setName("botonActualizarNotificaciones");
        actualizar.addActionListener(e -> actualizar());

        JPanel botones =
                new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(actualizar);
        inferior.add(botones, BorderLayout.EAST);
        add(inferior, BorderLayout.SOUTH);
    }

    private void actualizar() {
        int solicitud = ++versionListado;
        modelo.setRowCount(0);

        if (!disponiblePara(usuario)) {
            mensaje.setText(
                    "Esta pantalla solo está disponible para pacientes.");
            return;
        }

        mensaje.setText("Cargando notificaciones...");
        ejecutar(
                () -> servicio.listarMisNotificaciones(usuario),
                notificaciones -> {
                    if (solicitud == versionListado) {
                        presentarListado(notificaciones);
                    }
                },
                error -> {
                    if (solicitud == versionListado) {
                        modelo.setRowCount(0);
                        mensaje.setText(
                                "Error al cargar las notificaciones: "
                                + error.getMessage());
                    }
                });
    }

    private void presentarListado(
            List<Notificacion> notificaciones) {
        if (notificaciones == null) {
            throw new IllegalStateException(
                    "El servicio devolvió un listado nulo");
        }

        modelo.setRowCount(0);
        for (Notificacion notificacion : notificaciones) {
            modelo.addRow(new Object[] {
                    notificacion.getTitulo(),
                    notificacion.getMensaje(),
                    notificacion.getFechaHora(),
                    Boolean.TRUE.equals(notificacion.getLeida())
                            ? "Leída" : "No leída"
            });
        }

        mensaje.setText(notificaciones.isEmpty()
                ? "No tienes notificaciones."
                : notificaciones.size() + " notificación(es)");
    }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar() throws Exception;
    }

    private <T> void ejecutar(
            Operacion<T> operacion,
            Consumer<T> correcto,
            Consumer<Exception> fallo) {
        if (!asincrono) {
            try {
                correcto.accept(operacion.ejecutar());
            } catch (Exception e) {
                fallo.accept(e);
            }
            return;
        }

        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return operacion.ejecutar();
            }

            @Override
            protected void done() {
                try {
                    correcto.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fallo.accept(e);
                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();
                    if (causa instanceof Exception error) {
                        fallo.accept(error);
                    } else {
                        fallo.accept(new IllegalStateException(
                                "Error inesperado", causa));
                    }
                }
            }
        }.execute();
    }
}
