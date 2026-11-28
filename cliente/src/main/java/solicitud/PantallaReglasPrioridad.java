package solicitud;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import paciente.Rol;
import paciente.ServicioNoDisponibleException;
import paciente.SinPermisoException;
import paciente.Usuario;

public class PantallaReglasPrioridad extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ReglasPrioridadService servicio;
    private final Usuario usuario;
    private final ValidadorReglaPrioridad validador =
            new ValidadorReglaPrioridad();

    private final JTextField nombre = new JTextField(24);
    private final JTextField descripcion = new JTextField(24);
    private final JTextField orden = new JTextField(8);
    private final DefaultTableModel modelo = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Descripción", "Orden", "Estado"},
            0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modelo);
    private final JButton nuevo = new JButton("Nuevo");
    private final JButton guardar = new JButton("Crear regla");
    private final JButton desactivar = new JButton("Desactivar");
    private final JButton actualizar = new JButton("Actualizar");
    private final JLabel mensaje = new JLabel(" ");

    private List<ReglaPrioridad> mostradas = new ArrayList<>();
    private ReglaPrioridad seleccionada;

    public PantallaReglasPrioridad(
            ReglasPrioridadService servicio,
            Usuario usuario) {
        if (servicio == null) {
            throw new IllegalArgumentException(
                    "El servicio es obligatorio");
        }
        this.servicio = servicio;
        this.usuario = usuario;
        construir();
        if (disponiblePara(usuario)) {
            cargar();
        } else {
            mensaje.setText(
                    "Esta configuracion solo esta disponible "
                            + "para el Administrador del Sistema");
        }
    }

    public static boolean disponiblePara(Usuario usuario) {
        return usuario != null
                && usuario.rol() == Rol.ADMINISTRADOR;
    }

    private void construir() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        mensaje.setName("mensaje");

        if (!disponiblePara(usuario)) {
            add(mensaje, BorderLayout.CENTER);
            return;
        }

        JPanel formulario = new JPanel(new GridLayout(0, 2, 6, 6));
        campo(formulario, "Nombre", "nombre", nombre);
        campo(formulario, "Descripción", "descripcion", descripcion);
        campo(formulario, "Orden", "orden", orden);
        add(formulario, BorderLayout.NORTH);

        tabla.setName("tabla");
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                mostrarSeleccionada();
            }
        });
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        nuevo.setName("nuevo");
        guardar.setName("guardar");
        desactivar.setName("desactivar");
        actualizar.setName("actualizar");
        nuevo.addActionListener(evento -> nuevo());
        guardar.addActionListener(evento -> guardar());
        desactivar.addActionListener(evento -> desactivar());
        actualizar.addActionListener(evento -> cargar());

        JPanel botones = new JPanel();
        botones.add(nuevo);
        botones.add(guardar);
        botones.add(desactivar);
        botones.add(actualizar);

        JPanel pie = new JPanel(new BorderLayout());
        pie.add(botones, BorderLayout.NORTH);
        pie.add(mensaje, BorderLayout.SOUTH);
        add(pie, BorderLayout.SOUTH);
        actualizarBotones();
    }

    private static void campo(
            JPanel formulario,
            String etiqueta,
            String nombreCampo,
            JTextField campo) {
        campo.setName(nombreCampo);
        formulario.add(new JLabel(etiqueta));
        formulario.add(campo);
    }

    private void cargar() {
        try {
            List<ReglaPrioridad> resultado = servicio.listar(usuario);
            if (resultado == null) {
                throw new ServicioNoDisponibleException(
                        "El listado recibido es nulo");
            }
            mostradas = new ArrayList<>(resultado);
            modelo.setRowCount(0);
            for (ReglaPrioridad regla : mostradas) {
                modelo.addRow(new Object[]{
                        regla.getId(),
                        regla.getNombre(),
                        regla.getDescripcion(),
                        regla.getOrden(),
                        regla.isActivo() ? "Activa" : "Inactiva"
                });
            }
            mensaje.setForeground(Color.BLACK);
            mensaje.setText(mostradas.isEmpty()
                    ? "No hay reglas de prioridad configuradas."
                    : mostradas.size()
                            + " regla(s) de prioridad configurada(s).");
        } catch (SinPermisoException
                | IllegalArgumentException
                | ServicioNoDisponibleException e) {
            mostrarError(e.getMessage());
        }
        actualizarBotones();
    }

    private void mostrarSeleccionada() {
        int fila = tabla.getSelectedRow();
        seleccionada = fila >= 0 && fila < mostradas.size()
                ? mostradas.get(fila) : null;
        if (seleccionada == null) {
            limpiarCampos();
        } else {
            nombre.setText(seleccionada.getNombre());
            descripcion.setText(seleccionada.getDescripcion());
            orden.setText(Integer.toString(seleccionada.getOrden()));
        }
        actualizarBotones();
    }

    private void nuevo() {
        tabla.clearSelection();
        seleccionada = null;
        limpiarCampos();
        mensaje.setForeground(Color.BLACK);
        mensaje.setText("Introduce los datos de la nueva regla.");
        actualizarBotones();
    }

    private void guardar() {
        ValidadorReglaPrioridad.Resultado resultado =
                validador.validar(
                        nombre.getText(),
                        descripcion.getText(),
                        orden.getText());
        if (!resultado.esValido()) {
            mostrarError(String.join(" ", resultado.getErrores()));
            return;
        }

        try {
            if (seleccionada == null) {
                servicio.crear(usuario, resultado.getDatos());
                limpiarCampos();
                cargar();
                mostrarExito("Regla creada correctamente.");
            } else {
                servicio.modificar(
                        usuario,
                        seleccionada.getId(),
                        resultado.getDatos());
                seleccionada = null;
                limpiarCampos();
                cargar();
                mostrarExito("Regla modificada correctamente.");
            }
        } catch (SinPermisoException
                | IllegalArgumentException
                | ServicioNoDisponibleException
                | java.util.NoSuchElementException e) {
            mostrarError(e.getMessage());
        }
        actualizarBotones();
    }

    private void desactivar() {
        if (seleccionada == null || !seleccionada.isActivo()
                || !confirmarDesactivacion(seleccionada)) {
            return;
        }
        try {
            servicio.desactivar(usuario, seleccionada.getId());
            seleccionada = null;
            limpiarCampos();
            cargar();
            mostrarExito("Regla desactivada correctamente.");
        } catch (SinPermisoException
                | IllegalArgumentException
                | ServicioNoDisponibleException
                | java.util.NoSuchElementException e) {
            mostrarError(e.getMessage());
        }
        actualizarBotones();
    }

    protected boolean confirmarDesactivacion(
            ReglaPrioridad regla) {
        return JOptionPane.showConfirmDialog(
                this,
                "¿Quieres desactivar la regla \""
                        + regla.getNombre() + "\"?",
                "Confirmar desactivación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void actualizarBotones() {
        guardar.setText(seleccionada == null
                ? "Crear regla" : "Guardar cambios");
        desactivar.setEnabled(
                seleccionada != null && seleccionada.isActivo());
    }

    private void limpiarCampos() {
        nombre.setText("");
        descripcion.setText("");
        orden.setText("");
    }

    private void mostrarExito(String texto) {
        mensaje.setForeground(new Color(0, 120, 0));
        mensaje.setText(texto);
    }

    private void mostrarError(String texto) {
        mensaje.setForeground(new Color(180, 0, 0));
        mensaje.setText("Error: "
                + (texto == null ? "No se pudo completar la operación."
                        : texto));
    }
}
