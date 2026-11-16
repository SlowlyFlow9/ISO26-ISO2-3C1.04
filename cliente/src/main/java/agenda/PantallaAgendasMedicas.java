package agenda;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import paciente.Usuario;

public class PantallaAgendasMedicas extends JPanel {

    private final AgendasService servicio;
    private final Usuario usuario;

    private final JTextField medicoId =
            new JTextField();

    private final JTextField fecha =
            new JTextField();

    private final JTextField horaInicio =
            new JTextField();

    private final JTextField horaFin =
            new JTextField();

    private final JTextField observaciones =
            new JTextField();

    private final DefaultTableModel modelo =
            new DefaultTableModel(
                    new Object[]{
                            "ID",
                            "Médico",
                            "Fecha",
                            "Inicio",
                            "Fin",
                            "Activo"
                    },
                    0);

    private final JTable tabla =
            new JTable(modelo);

    public PantallaAgendasMedicas(
            AgendasService servicio,
            Usuario usuario) {

        this.servicio = servicio;
        this.usuario = usuario;

        construir();
    }

    private void construir() {

        setLayout(new BorderLayout());

        JPanel formulario =
                new JPanel(new GridLayout(5, 2));

        formulario.add(
                new JLabel("ID médico"));
        formulario.add(medicoId);

        formulario.add(
                new JLabel("Fecha (AAAA-MM-DD)"));
        formulario.add(fecha);

        formulario.add(
                new JLabel("Hora inicio (HH:mm)"));
        formulario.add(horaInicio);

        formulario.add(
                new JLabel("Hora fin (HH:mm)"));
        formulario.add(horaFin);

        formulario.add(
                new JLabel("Observaciones"));
        formulario.add(observaciones);

        JPanel botones = new JPanel();

        JButton crear =
                new JButton("Crear");

        JButton modificar =
                new JButton("Modificar");

        JButton eliminar =
                new JButton("Eliminar");

        JButton consultar =
                new JButton("Actualizar");

        botones.add(crear);
        botones.add(modificar);
        botones.add(eliminar);
        botones.add(consultar);

        crear.addActionListener(
                e -> crear());

        modificar.addActionListener(
                e -> modificar());

        eliminar.addActionListener(
                e -> eliminar());

        consultar.addActionListener(
                e -> actualizar());

        add(formulario, BorderLayout.NORTH);
        add(new JScrollPane(tabla),
                BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        actualizar();
    }

    private AgendaMedica leerFormulario() {

        AgendaMedica agenda =
                new AgendaMedica();

        agenda.setMedicoId(
                Long.parseLong(
                        medicoId.getText().trim()));

        agenda.setFecha(
                LocalDate.parse(
                        fecha.getText().trim()));

        agenda.setHoraInicio(
                LocalTime.parse(
                        horaInicio.getText().trim()));

        agenda.setHoraFin(
                LocalTime.parse(
                        horaFin.getText().trim()));

        agenda.setObservaciones(
                observaciones.getText());

        return agenda;
    }

    private void crear() {

        try {
            servicio.crear(
                    usuario,
                    leerFormulario());

            actualizar();

            mensaje("Agenda creada correctamente");

        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void modificar() {

        Long id = idSeleccionado();

        if (id == null) {
            error("Seleccione una agenda");
            return;
        }

        try {
            servicio.modificar(
                    usuario,
                    id,
                    leerFormulario());

            actualizar();

            mensaje("Agenda modificada correctamente");

        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void eliminar() {

        Long id = idSeleccionado();

        if (id == null) {
            error("Seleccione una agenda");
            return;
        }

        try {
            servicio.eliminar(
                    usuario,
                    id);

            actualizar();

            mensaje("Agenda eliminada correctamente");

        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private Long idSeleccionado() {

        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            return null;
        }

        return ((Number)
                modelo.getValueAt(fila, 0))
                .longValue();
    }

    private void actualizar() {

        try {
            List<AgendaMedica> agendas =
                    servicio.listar(
                            usuario,
                            null,
                            null,
                            null);

            modelo.setRowCount(0);

            for (AgendaMedica agenda : agendas) {

                modelo.addRow(
                        new Object[]{
                                agenda.getId(),
                                agenda.getMedicoId(),
                                agenda.getFecha(),
                                agenda.getHoraInicio(),
                                agenda.getHoraFin(),
                                agenda.isActivo()
                        });
            }

        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void mensaje(String texto) {

        JOptionPane.showMessageDialog(
                this,
                texto);
    }

    private void error(String texto) {

        JOptionPane.showMessageDialog(
                this,
                texto,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}