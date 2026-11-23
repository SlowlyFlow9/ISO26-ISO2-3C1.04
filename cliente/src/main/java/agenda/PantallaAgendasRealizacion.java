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
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import paciente.Rol;
import paciente.Usuario;

/** Pantalla de CU14 para que el médico gestione sus agendas. */
public class PantallaAgendasRealizacion extends JPanel {

    private final AgendasRealizacionService servicio;
    private final Usuario usuario;

    private final JTextField fecha = new JTextField();
    private final JTextField horaInicio = new JTextField();
    private final JTextField horaFin = new JTextField();
    private final JTextField observaciones = new JTextField();

    private final DefaultTableModel modelo =
            new DefaultTableModel(
                    new Object[]{
                            "ID",
                            "Fecha",
                            "Inicio",
                            "Fin",
                            "Activa"
                    },
                    0) {
                @Override
                public boolean isCellEditable(
                        int fila,
                        int columna) {
                    return false;
                }
            };

    private final JTable tabla = new JTable(modelo);

    public PantallaAgendasRealizacion(
            AgendasRealizacionService servicio,
            Usuario usuario) {

        if (servicio == null) {
            throw new IllegalArgumentException(
                    "El servicio es obligatorio");
        }
        if (!disponiblePara(usuario)) {
            throw new IllegalArgumentException(
                    "La pantalla solo está disponible para un médico identificado");
        }

        this.servicio = servicio;
        this.usuario = usuario;
        construir();
        actualizar();
    }

    public static boolean disponiblePara(Usuario usuario) {
        return usuario != null
                && usuario.rol() == Rol.MEDICO
                && usuario.medicoId() != null
                && usuario.medicoId() > 0;
    }

    private void construir() {
        setLayout(new BorderLayout());
        tabla.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        JPanel formulario = new JPanel(new GridLayout(4, 2));
        formulario.add(new JLabel("Fecha (AAAA-MM-DD)"));
        formulario.add(fecha);
        formulario.add(new JLabel("Hora inicio (HH:mm)"));
        formulario.add(horaInicio);
        formulario.add(new JLabel("Hora fin (HH:mm)"));
        formulario.add(horaFin);
        formulario.add(new JLabel("Observaciones"));
        formulario.add(observaciones);

        JButton crear = new JButton("Crear");
        JButton modificar = new JButton("Modificar");
        JButton eliminar = new JButton("Eliminar");
        JButton actualizar = new JButton("Actualizar");

        crear.addActionListener(evento -> crear());
        modificar.addActionListener(evento -> modificar());
        eliminar.addActionListener(evento -> eliminar());
        actualizar.addActionListener(evento -> actualizar());

        JPanel botones = new JPanel();
        botones.add(crear);
        botones.add(modificar);
        botones.add(eliminar);
        botones.add(actualizar);

        add(formulario, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
    }

    private AgendaMedica leerFormulario()
            throws DatosAgendaInvalidosException {

        AgendaMedica agenda = new AgendaMedica();
        agenda.setMedicoId(usuario.medicoId());
        agenda.setFecha(LocalDate.parse(fecha.getText().trim()));
        agenda.setHoraInicio(
                LocalTime.parse(horaInicio.getText().trim()));
        agenda.setHoraFin(
                LocalTime.parse(horaFin.getText().trim()));
        agenda.setObservaciones(observaciones.getText());

        List<String> errores =
                ValidadorAgendaMedica.validar(agenda);
        if (!errores.isEmpty()) {
            throw new DatosAgendaInvalidosException(errores);
        }

        return agenda;
    }

    private void crear() {
        try {
            servicio.crearPropia(usuario, leerFormulario());
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
            servicio.modificarPropia(
                    usuario, id, leerFormulario());
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
            servicio.eliminarPropia(usuario, id);
            actualizar();
            mensaje("Agenda eliminada correctamente");
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    private void actualizar() {
        try {
            List<AgendaMedica> agendas =
                    servicio.listarPropias(usuario);
            modelo.setRowCount(0);

            for (AgendaMedica agenda : agendas) {
                modelo.addRow(new Object[]{
                        agenda.getId(),
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

    private Long idSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return null;
        }
        return ((Number) modelo.getValueAt(fila, 0)).longValue();
    }

    private void mensaje(String texto) {
        JOptionPane.showMessageDialog(this, texto);
    }

    private void error(String texto) {
        JOptionPane.showMessageDialog(
                this,
                texto == null ? "Error al gestionar la agenda" : texto,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
