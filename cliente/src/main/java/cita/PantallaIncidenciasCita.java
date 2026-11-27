package cita;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

/** Interfaz médica para CU20. */
public class PantallaIncidenciasCita extends JPanel {

    private static final long serialVersionUID = 1L;

    private final CitasIncidenciasService servicio;
    private final Usuario usuario;
    private final JTextField campoCitaId = new JTextField();
    private final JTextArea campoDescripcion = new JTextArea(3, 30);
    private final JLabel mensaje = new JLabel(" ");

    private final DefaultTableModel modelo =
            new DefaultTableModel(
                    new Object[]{"Fecha y hora", "Descripción"}, 0) {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean isCellEditable(
                        int fila,
                        int columna) {
                    return false;
                }
            };

    private final JTable tabla = new JTable(modelo);

    public PantallaIncidenciasCita(
            CitasIncidenciasService servicio,
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
        campoDescripcion.setName("campoDescripcion");
        mensaje.setName("mensaje");
        tabla.setName("tablaIncidencias");
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel formulario = new JPanel(new GridLayout(0, 1, 4, 4));
        formulario.add(new JLabel("ID de la cita"));
        formulario.add(campoCitaId);
        formulario.add(new JLabel("Descripción de la incidencia"));
        formulario.add(new JScrollPane(campoDescripcion));

        JButton registrar = new JButton("Registrar incidencia");
        registrar.setName("botonRegistrarIncidencia");
        registrar.addActionListener(evento -> registrar());
        JButton consultar = new JButton("Consultar historial");
        consultar.setName("botonConsultarIncidencias");
        consultar.addActionListener(evento -> consultar());

        JPanel acciones = new JPanel();
        acciones.add(registrar);
        acciones.add(consultar);

        JPanel superior = new JPanel(new BorderLayout(4, 4));
        superior.add(formulario, BorderLayout.CENTER);
        superior.add(acciones, BorderLayout.SOUTH);

        add(superior, BorderLayout.NORTH);
        add(new JScrollPane(tabla), BorderLayout.CENTER);
        add(mensaje, BorderLayout.SOUTH);
    }

    private void registrar() {
        try {
            long citaId = leerCitaId();
            String descripcion = campoDescripcion.getText();
            servicio.registrarIncidencia(
                    usuario, citaId, descripcion);
            campoDescripcion.setText("");
            mensaje.setText("Incidencia registrada correctamente");
            cargarHistorial(citaId, true);
        } catch (SinPermisoException
                | CitaNoEncontradaException
                | CitaNoIncidenciableException
                | IllegalArgumentException
                | ServicioNoDisponibleException e) {
            mostrarError(e);
        }
    }

    private void consultar() {
        try {
            cargarHistorial(leerCitaId(), false);
        } catch (IllegalArgumentException
                | ServicioNoDisponibleException e) {
            mostrarError(e);
        }
    }

    private void cargarHistorial(
            long citaId,
            boolean incidenciaRegistrada) {
        try {
            List<IncidenciaCita> incidencias =
                    servicio.listarIncidencias(usuario, citaId);
            modelo.setRowCount(0);
            for (IncidenciaCita incidencia : incidencias) {
                modelo.addRow(new Object[]{
                        incidencia.getFechaHora(),
                        incidencia.getDescripcion()
                });
            }
        } catch (SinPermisoException
                | CitaNoEncontradaException
                | ServicioNoDisponibleException e) {
            modelo.setRowCount(0);
            if (incidenciaRegistrada) {
                mensaje.setText(
                        "Incidencia registrada, pero no se pudo "
                                + "actualizar el historial: "
                                + textoError(e));
            } else {
                mostrarError(e);
            }
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

    private void mostrarError(Exception error) {
        mensaje.setText("Error: " + textoError(error));
    }

    private String textoError(Exception error) {
        return error.getMessage() == null
                ? "operación fallida"
                : error.getMessage();
    }
}
