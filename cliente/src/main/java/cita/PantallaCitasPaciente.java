package cita;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU01 - Consultar citas del paciente.
 * CU02 - Confirmar citas programadas.
 *
 * La pantalla depende del contrato CitasService.
 */

public class PantallaCitasPaciente extends JPanel {

    private final CitasService servicio;
    private final Usuario usuario;
    private final boolean asincrono;
    private final BiPredicate<Component, Long> pedirConfirmacion;

    private int versionListado;
    private int versionDetalle;
    private boolean confirmando;

    private final DefaultTableModel modelo =
            new DefaultTableModel(
                    new Object[] {
                            "ID",
                            "Fecha y hora",
                            "Médico",
                            "Tipo de prueba",
                            "Estado"
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

    private final JButton botonConfirmar =
            new JButton("Confirmar cita");

    /**
     * Constructor original de CU01. Conserva el funcionamiento de los
     * tests existentes.
     */
    public PantallaCitasPaciente(
            CitasService servicio,
            Usuario usuario) {

        this(servicio, usuario, false,
                dialogoNormal());
    }

     //Constructor para la aplicación real.
    public PantallaCitasPaciente(
            CitasService servicio,
            Usuario usuario,
            boolean asincrono) {

        this(servicio, usuario, asincrono,
                dialogoNormal());
    }

     //Constructor utilizado por las pruebas de CU02
    PantallaCitasPaciente(
            CitasService servicio,
            Usuario usuario,
            BiPredicate<Component, Long> pedirConfirmacion) {

        this(servicio, usuario, false,
                pedirConfirmacion);
    }

    private PantallaCitasPaciente(
            CitasService servicio,
            Usuario usuario,
            boolean asincrono,
            BiPredicate<Component, Long> pedirConfirmacion) {

        this.servicio = servicio;
        this.usuario = usuario;
        this.asincrono = asincrono;
        this.pedirConfirmacion = pedirConfirmacion;

        construir();
        actualizar();
    }

    private static BiPredicate<Component, Long>
            dialogoNormal() {

        return (padre, id) ->
                JOptionPane.showConfirmDialog(
                        padre,
                        "¿Quieres confirmar la cita " + id + "?",
                        "Confirmar cita",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE)
                        == JOptionPane.YES_OPTION;
    }

    public static boolean disponiblePara(
            Usuario usuario) {

        return usuario != null
                && usuario.rol() == Rol.PACIENTE;
    }

    private void construir() {

        setLayout(new BorderLayout(10, 10));

        add(new JLabel("Mis citas"), BorderLayout.NORTH);

        tabla.setName("tablaCitas");
        tabla.setSelectionMode(
                javax.swing.ListSelectionModel.SINGLE_SELECTION);

        tabla.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        mostrarSeleccion();
                    }
                });

        add(new JScrollPane(tabla), BorderLayout.CENTER);

        detalle.setName("detalleCita");
        detalle.setEditable(false);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(true);
        detalle.setPreferredSize(new Dimension(260, 200));

        add(new JScrollPane(detalle), BorderLayout.EAST);

        JPanel inferior = new JPanel(new BorderLayout());

        mensaje.setName("mensajeCitas");
        inferior.add(mensaje, BorderLayout.CENTER);

        JButton actualizar = new JButton("Actualizar");
        actualizar.setName("botonActualizar");
        actualizar.addActionListener(e -> actualizar());

        botonConfirmar.setName("botonConfirmarCita");
        botonConfirmar.setEnabled(false);
        botonConfirmar.addActionListener(
                e -> confirmarSeleccion());

        JPanel botones =
                new JPanel(new FlowLayout(FlowLayout.RIGHT));

        botones.add(botonConfirmar);
        botones.add(actualizar);

        inferior.add(botones, BorderLayout.EAST);
        add(inferior, BorderLayout.SOUTH);
    }

   //CU01: obtiene el listado de citas.
    private void actualizar() {
        actualizar(null, null);
    }

    private void actualizar(
            Long citaSeleccionada,
            String mensajeExito) {

        int solicitud = ++versionListado;
        ++versionDetalle;

        botonConfirmar.setEnabled(false);
        mensaje.setText("Cargando citas...");

        ejecutar(
                () -> servicio.listarMisCitas(usuario),

                citas -> {
                    if (solicitud != versionListado) {
                        return;
                    }

                    presentarListado(
                            citas, citaSeleccionada,
                            mensajeExito);
                },

                error -> {
                    if (solicitud != versionListado) {
                        return;
                    }

                    modelo.setRowCount(0);
                    detalle.setText("");
                    botonConfirmar.setEnabled(false);
                    mensaje.setText(
                            "Error: " + error.getMessage());
                }
        );
    }

    private void presentarListado(
            List<Cita> citas,
            Long citaSeleccionada,
            String mensajeExito) {

        modelo.setRowCount(0);
        detalle.setText("");

        for (Cita cita : citas) {

            modelo.addRow(new Object[] {
                    cita.getId(),
                    cita.getFechaHora(),
                    cita.getMedicoId(),
                    cita.getTipoPruebaId(),
                    cita.getEstado()
            });
        }

        if (citaSeleccionada != null) {

            for (int i = 0; i < modelo.getRowCount(); i++) {

                long id = ((Number)
                        modelo.getValueAt(i, 0)).longValue();

                if (id == citaSeleccionada) {
                    tabla.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }

        mensaje.setText(
                mensajeExito != null
                        ? mensajeExito
                        : citas.size() + " cita(s)");
    }

     //CU01: consulta los detalles de la cita seleccionada.
    private void mostrarSeleccion() {

        int solicitud = ++versionDetalle;
        int fila = tabla.getSelectedRow();

        botonConfirmar.setEnabled(false);

        if (fila < 0) {
            detalle.setText("");
            return;
        }

        long id = ((Number)
                modelo.getValueAt(fila, 0)).longValue();

        detalle.setText("Cargando detalles...");

        ejecutar(
                () -> servicio.consultarMiCita(usuario, id),

                cita -> {
                    if (solicitud != versionDetalle) {
                        return;
                    }

                    detalle.setText(
                            "Cita " + cita.getId()
                            + "\nFecha y hora: "
                            + cita.getFechaHora()
                            + "\nMédico: "
                            + cita.getMedicoId()
                            + "\nTipo de prueba: "
                            + cita.getTipoPruebaId()
                            + "\nEstado: "
                            + cita.getEstado());

                    botonConfirmar.setEnabled(
                            !confirmando
                            && cita.getEstado()
                                    == EstadoCita.PROGRAMADA);
                },

                error -> {
                    if (solicitud != versionDetalle) {
                        return;
                    }

                    detalle.setText("");
                    botonConfirmar.setEnabled(false);
                    mensaje.setText(
                            "Error: " + error.getMessage());
                }
        );
    }

     //CU02: confirma la cita seleccionada. El servicio realiza las validaciones
     //de negocio
    private void confirmarSeleccion() {

        int fila = tabla.getSelectedRow();

        if (confirmando || fila < 0
                || modelo.getValueAt(fila, 4)
                        != EstadoCita.PROGRAMADA) {
            return;
        }

        long citaId = ((Number)
                modelo.getValueAt(fila, 0)).longValue();

        if (!pedirConfirmacion.test(this, citaId)) {
            return;
        }

        confirmando = true;
        botonConfirmar.setEnabled(false);
        mensaje.setText("Confirmando cita...");

        ejecutar(
                () -> servicio.confirmarMiCita(
                        usuario, citaId),

                confirmada -> {
                    confirmando = false;

                    actualizar(
                            citaId,
                            "Cita " + citaId
                            + " confirmada correctamente");
                },

                error -> {
                    confirmando = false;

                    mensaje.setText(
                            "Error al confirmar: "
                            + error.getMessage());

                    mostrarSeleccion();
                }
        );
    }

     // ejecuta las operaciones del servicio.
     //En la aplicación HTTP utiliza SwingWorker y en los tests existentes
     //mantiene ejecución directa.
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
                        fallo.accept(
                                new IllegalStateException(
                                        "Error inesperado", causa));
                    }
                }
            }
        }.execute();
    }
}
