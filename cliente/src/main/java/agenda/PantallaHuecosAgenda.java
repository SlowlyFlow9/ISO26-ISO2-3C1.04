package agenda;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import paciente.Rol;
import paciente.Usuario;

/**
 * CU29 - Pantalla Swing de gestión de huecos.
 *
 * Permite consultar, crear, bloquear y liberar
 * huecos de una agenda médica.
 */

public class PantallaHuecosAgenda extends JPanel {

    private final HuecosAgendaService servicio;
    private final Usuario usuario;

    // Campos del formulario.
    private final JTextField campoAgendaId = new JTextField();
    private final JTextField campoHoraInicio = new JTextField();
    private final JTextField campoHoraFin = new JTextField();

    // Tabla de huecos.
    private final DefaultTableModel modelo =
            new DefaultTableModel(
                    new Object[]{
                            "ID", "Inicio", "Fin", "Estado"
                    }, 0) {

                @Override
                public boolean isCellEditable(
                        int fila, int columna) {
                    return false;
                }
            };

    private final JTable tabla = new JTable(modelo);

    // Botones.
    private final JButton botonConsultar =
            new JButton("Consultar");

    private final JButton botonCrear =
            new JButton("Crear hueco");

    private final JButton botonBloquear =
            new JButton("Bloquear");

    private final JButton botonLiberar =
            new JButton("Liberar");

    private boolean ocupado;

    public PantallaHuecosAgenda(
            HuecosAgendaService servicio,
            Usuario usuario) {

        if (servicio == null) {
            throw new IllegalArgumentException(
                    "El servicio es obligatorio");
        }

        this.servicio = servicio;
        this.usuario = usuario;

        construir();
    }

    private void construir() {

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));

        // FORMULARIO
        JPanel formulario =
                new JPanel(new GridLayout(3, 2, 8, 8));

        formulario.add(new JLabel("ID de agenda"));
        formulario.add(campoAgendaId);

        formulario.add(new JLabel("Hora inicio (HH:mm)"));
        formulario.add(campoHoraInicio);

        formulario.add(new JLabel("Hora fin (HH:mm)"));
        formulario.add(campoHoraFin);

        JPanel superior = new JPanel(
                new BorderLayout(0, 8));

        superior.add(
                new JLabel("CU29 - Gestión de huecos"),
                BorderLayout.NORTH);

        superior.add(
                formulario, BorderLayout.CENTER);

        add(superior, BorderLayout.NORTH);

        // TABLA
        tabla.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        tabla.setAutoCreateRowSorter(true);

        tabla.getSelectionModel()
                .addListSelectionListener(
                        e -> actualizarBotones());

        add(new JScrollPane(tabla),
                BorderLayout.CENTER);

        // BOTONES
        JPanel botones =
                new JPanel(new FlowLayout());

        botones.add(botonConsultar);
        botones.add(botonCrear);
        botones.add(botonBloquear);
        botones.add(botonLiberar);

        add(botones, BorderLayout.SOUTH);

        // Asociar acciones.
        botonConsultar.addActionListener(
                e -> consultar());

        botonCrear.addActionListener(
                e -> crear());

        botonBloquear.addActionListener(
                e -> cambiarBloqueo(true));

        botonLiberar.addActionListener(
                e -> cambiarBloqueo(false));

        actualizarBotones();
    }

    // Comprueba los permisos para habilitar botones.
    private void actualizarBotones() {

        boolean permitido =
                usuario != null
                && usuario.rol()
                        == Rol.PERSONAL_ADMINISTRATIVO;

        boolean habilitado = permitido && !ocupado;

        botonConsultar.setEnabled(habilitado);
        botonCrear.setEnabled(habilitado);

        boolean seleccionado =
                tabla.getSelectedRow() >= 0;

        botonBloquear.setEnabled(
                habilitado && seleccionado);

        botonLiberar.setEnabled(
                habilitado && seleccionado);
    }

    // Lee y valida el ID de la agenda
    private long leerAgendaId() {

        long id;

        try {
            id = Long.parseLong(
                    campoAgendaId.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Introduzca un ID de agenda válido");
        }

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID de agenda debe ser positivo");
        }

        return id;
    }

    // Lee una hora en formato HH:mm.
    private LocalTime leerHora(
            JTextField campo, String nombre) {

        try {
            return LocalTime.parse(
                    campo.getText().trim());

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    nombre + " debe tener formato HH:mm");
        }
    }

    // Construye y valida los datos del hueco.
    private HuecoAgenda leerFormulario() {

        HuecoAgenda datos = new HuecoAgenda();

        datos.setAgendaId(leerAgendaId());

        datos.setHoraInicio(
                leerHora(campoHoraInicio,
                        "La hora de inicio"));

        datos.setHoraFin(
                leerHora(campoHoraFin,
                        "La hora de fin"));

        List<String> errores =
                ValidadorHuecoAgenda.validar(datos);

        if (!errores.isEmpty()) {
            throw new IllegalArgumentException(
                    String.join("\n", errores));
        }

        return datos;
    }

    // CONSULTAR
    private void consultar() {

        try {
            consultarAgenda(leerAgendaId());

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    private void consultarAgenda(long agendaId) {

        ejecutarOperacion(
                () -> servicio.listarPorAgenda(
                        usuario, agendaId),
                this::mostrarHuecos);
    }

    // Mostrar los huecos recibidos del servidor.
    private void mostrarHuecos(List<HuecoAgenda> huecos) {

        modelo.setRowCount(0);

        for (HuecoAgenda hueco : huecos) {

            String estado = hueco.isBloqueado()
                    ? "Bloqueado"
                    : "Disponible";

            modelo.addRow(new Object[]{
                    hueco.getId(),
                    hueco.getHoraInicio(),
                    hueco.getHoraFin(),
                    estado
            });
        }

        actualizarBotones();
    }

    // CREAR
    private void crear() {

        try {
            HuecoAgenda datos = leerFormulario();

            ejecutarOperacion(
                    () -> servicio.crear(usuario, datos),
                    creado -> {

                        mostrarMensaje(
                                "Hueco creado correctamente");

                        consultarAgenda(creado.getAgendaId());
                    });

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    // Recupera el identificador del hueco seleccionado.
    private long idSeleccionado() {

        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            throw new IllegalArgumentException(
                    "Seleccione primero un hueco");
        }

        int filaModelo =
                tabla.convertRowIndexToModel(fila);

        return ((Number)
                modelo.getValueAt(filaModelo, 0))
                .longValue();
    }

    // BLOQUEAR o LIBERAR
    private void cambiarBloqueo(boolean bloquear) {

        try {
            long id = idSeleccionado();

            String accion = bloquear
                    ? "bloquear" : "liberar";

            int confirmacion =
                    JOptionPane.showConfirmDialog(
                            this,
                            "¿Desea " + accion
                                    + " el hueco " + id + "?",
                            "Confirmar operación",
                            JOptionPane.YES_NO_OPTION);

            if (confirmacion != JOptionPane.YES_OPTION) {
                return;
            }

            ejecutarOperacion(
                    () -> bloquear
                            ? servicio.bloquear(usuario, id)
                            : servicio.liberar(usuario, id),
                    actualizado -> {

                        mostrarMensaje(
                                "Hueco actualizado correctamente");

                        consultarAgenda(
                                actualizado.getAgendaId());
                    });

        } catch (RuntimeException e) {
            mostrarError(e.getMessage());
        }
    }

    //Ejecuta las peticiones HTTP en segundo plano, y así la pantalla no se congela
    //mientras espera la respuesta del servidor.

    private <T> void ejecutarOperacion(
            Callable<T> operacion,
            Consumer<T> alFinalizar) {

        ocupado = true;
        actualizarBotones();

        new SwingWorker<T, Void>() {

            @Override
            protected T doInBackground() throws Exception {
                return operacion.call();
            }

            @Override
            protected void done() {

                ocupado = false;
                actualizarBotones();

                try {
                    alFinalizar.accept(get());

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarError("Operación interrumpida");

                } catch (ExecutionException e) {
                    Throwable causa = e.getCause();

                    mostrarError(causa == null
                            ? "No se pudo realizar la operación"
                            : causa.getMessage());

                } catch (RuntimeException e) {
                    mostrarError(e.getMessage());
                }
            }

        }.execute();
    }

    private void mostrarMensaje(String texto) {
        JOptionPane.showMessageDialog(this, texto);
    }

    private void mostrarError(String texto) {

        JOptionPane.showMessageDialog(
                this,
                texto == null || texto.isBlank()
                        ? "Se ha producido un error"
                        : texto,
                "Error",
                JOptionPane.ERROR_MESSAGE);
    }

}
