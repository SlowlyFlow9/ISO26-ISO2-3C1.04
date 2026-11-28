package cita;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU06: Pantalla del historial de pruebas realizadas.
 */
public class PantallaHistorialPruebas extends JPanel {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final HistorialPruebasService servicio;
    private final Usuario usuario;
    private final boolean asincrono;

    private List<Cita> historial = List.of();

    private final DefaultTableModel modelo =
            new DefaultTableModel(
                    new Object[] {
                            "ID", "Fecha de cita",
                            "Medico", "Tipo de prueba", "Estado"
                    }, 0) {

                @Override
                public boolean isCellEditable(
                        int fila, int columna) {
                    return false;
                }
            };

    private final JTable tabla = new JTable(modelo);
    private final JTextArea detalle = new JTextArea();
    private final JLabel mensaje = new JLabel();

    private final JButton botonActualizar =
            new JButton("Actualizar");

    public PantallaHistorialPruebas(
            HistorialPruebasService servicio,
            Usuario usuario) {

        this(servicio, usuario, false);
    }

    public PantallaHistorialPruebas(
            HistorialPruebasService servicio,
            Usuario usuario,
            boolean asincrono) {

        if (servicio == null) {
            throw new IllegalArgumentException(
                    "El servicio es obligatorio");
        }

        this.servicio = servicio;
        this.usuario = usuario;
        this.asincrono = asincrono;

        construir();
        actualizar();
    }

    public static boolean disponiblePara(
            Usuario usuario) {

        return usuario != null
                && usuario.rol() == Rol.PACIENTE;
    }

    private void construir() {

        setLayout(new BorderLayout(10, 10));

        add(new JLabel("Mi historial de pruebas"),
                BorderLayout.NORTH);

        tabla.setName("tablaHistorial");
        tabla.setSelectionMode(
                javax.swing.ListSelectionModel.SINGLE_SELECTION);

        tabla.getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {
                        mostrarDetalle();
                    }
                });

        add(new JScrollPane(tabla), BorderLayout.CENTER);

        detalle.setName("detalleHistorial");
        detalle.setEditable(false);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(true);
        detalle.setPreferredSize(new Dimension(260, 200));

        add(new JScrollPane(detalle), BorderLayout.EAST);

        JPanel inferior = new JPanel(
                new BorderLayout());

        mensaje.setName("mensajeHistorial");
        inferior.add(mensaje, BorderLayout.CENTER);

        JPanel botones = new JPanel(
                new FlowLayout(FlowLayout.RIGHT));

        botonActualizar.setName(
                "botonActualizarHistorial");

        botonActualizar.addActionListener(
                e -> actualizar());

        botones.add(botonActualizar);
        inferior.add(botones, BorderLayout.EAST);

        add(inferior, BorderLayout.SOUTH);
    }

    public void actualizar() {

        botonActualizar.setEnabled(false);
        mensaje.setText("Cargando historial...");

        if (asincrono) {

            new SwingWorker<List<Cita>, Void>() {

                @Override
                protected List<Cita> doInBackground()
                        throws Exception {

                    return servicio.listarMiHistorial(usuario);
                }

                @Override
                protected void done() {

                    try {
                        presentar(get());

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        mostrarError(e);

                    } catch (ExecutionException e) {
                        mostrarError(e.getCause());

                    } finally {
                        botonActualizar.setEnabled(true);
                    }
                }

            }.execute();

        } else {

            try {
                presentar(servicio.listarMiHistorial(usuario));

            } catch (Exception e) {
                mostrarError(e);

            } finally {
                botonActualizar.setEnabled(true);
            }
        }
    }

    private void presentar(List<Cita> citas) {

        if (citas == null) {
            throw new IllegalArgumentException(
                    "El historial no puede ser nulo");
        }

        historial = List.copyOf(citas);

        modelo.setRowCount(0);
        detalle.setText("");

        for (Cita cita : historial) {

            modelo.addRow(new Object[] {
                    cita.getId(),
                    fecha(cita.getFechaHora()),
                    cita.getMedicoId(),
                    cita.getTipoPruebaId(),
                    cita.getEstado()
            });
        }

        if (historial.isEmpty()) {
            mensaje.setText(
                    "No tienes pruebas realizadas");
        } else {
            mensaje.setText(
                    "Pruebas realizadas: " + historial.size());
        }
    }

    private void mostrarDetalle() {

        int fila = tabla.getSelectedRow();

        if (fila < 0 || fila >= historial.size()) {
            detalle.setText("");
            return;
        }

        Cita cita = historial.get(
                tabla.convertRowIndexToModel(fila));

        detalle.setText(
                "Prueba " + cita.getId()
                + "\nFecha de cita: "
                + fecha(cita.getFechaHora())
                + "\nMedico: " + cita.getMedicoId()
                + "\nTipo de prueba: "
                + cita.getTipoPruebaId()
                + "\nEstado: " + cita.getEstado());
    }

    private void mostrarError(Throwable error) {

        historial = List.of();
        modelo.setRowCount(0);
        detalle.setText("");

        String texto = error.getMessage();

        mensaje.setText("Error: " +
                (texto == null
                        ? "No se pudo consultar el historial"
                        : texto));
    }

    private String fecha(LocalDateTime fecha) {

        return fecha == null
                ? ""
                : fecha.format(FORMATO);
    }
}
