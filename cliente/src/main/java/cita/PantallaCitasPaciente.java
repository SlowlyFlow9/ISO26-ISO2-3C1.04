package cita;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import paciente.Rol;
import paciente.Usuario;

public class PantallaCitasPaciente
        extends JPanel {

    private final CitasService servicio;
    private final Usuario usuario;

    private final DefaultTableModel modelo =
            new DefaultTableModel(
                    new Object[] {
                            "ID",
                            "Fecha y hora",
                            "Médico",
                            "Tipo de prueba",
                            "Estado"
                    },
                    0) {

                @Override
                public boolean isCellEditable(
                        int row,
                        int column) {

                    return false;
                }
            };

    private final JTable tabla =
            new JTable(modelo);

    private final JTextArea detalle =
            new JTextArea();

    private final JLabel mensaje =
            new JLabel();

    public PantallaCitasPaciente(
            CitasService servicio,
            Usuario usuario) {

        this.servicio = servicio;
        this.usuario = usuario;

        construir();
        actualizar();
    }

    public static boolean disponiblePara(
            Usuario usuario) {

        return usuario != null
                && usuario.rol() == Rol.PACIENTE;
    }

    private void construir() {

        setLayout(
                new BorderLayout(10, 10));

        JLabel titulo =
                new JLabel("Mis citas");

        add(
                titulo,
                BorderLayout.NORTH);

        tabla.setName("tablaCitas");
        tabla.setSelectionMode(
                javax.swing.ListSelectionModel
                        .SINGLE_SELECTION);

        tabla.getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {
                        mostrarSeleccion();
                    }
                });

        add(
                new JScrollPane(tabla),
                BorderLayout.CENTER);

        detalle.setName("detalleCita");
        detalle.setEditable(false);
        detalle.setLineWrap(true);
        detalle.setWrapStyleWord(true);
        detalle.setPreferredSize(
                new Dimension(
                        260,
                        200));

        add(
                new JScrollPane(detalle),
                BorderLayout.EAST);

        JPanel inferior =
                new JPanel(
                        new BorderLayout());

        JButton actualizar =
                new JButton("Actualizar");

        actualizar.setName(
                "botonActualizar");

        actualizar.addActionListener(
                e -> actualizar());

        mensaje.setName(
                "mensajeCitas");

        inferior.add(
                mensaje,
                BorderLayout.CENTER);

        inferior.add(
                actualizar,
                BorderLayout.EAST);

        add(
                inferior,
                BorderLayout.SOUTH);
    }

    private void actualizar() {

        try {

            List<Cita> citas =
                    servicio.listarMisCitas(
                            usuario);

            modelo.setRowCount(0);
            detalle.setText("");

            for (Cita cita : citas) {

                modelo.addRow(
                        new Object[] {
                                cita.getId(),
                                cita.getFechaHora(),
                                cita.getMedicoId(),
                                cita.getTipoPruebaId(),
                                cita.getEstado()
                        });
            }

            mensaje.setText(
                    citas.size()
                            + " cita(s)");

        } catch (Exception e) {

            modelo.setRowCount(0);
            detalle.setText("");

            mensaje.setText(
                    "Error: "
                            + e.getMessage());
        }
    }

    private void mostrarSeleccion() {

        int fila =
                tabla.getSelectedRow();

        if (fila < 0) {
            detalle.setText("");
            return;
        }

        long id =
                ((Number)
                        modelo.getValueAt(
                                fila,
                                0))
                        .longValue();

        try {

            Cita cita =
                    servicio.consultarMiCita(
                            usuario,
                            id);

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

        } catch (Exception e) {

            detalle.setText("");

            mensaje.setText(
                    "Error: "
                            + e.getMessage());
        }
    }
}