package solicitud;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import paciente.Rol;
import paciente.Usuario;
import tipoprueba.TipoPrueba;

/**
 * CU11 - Pantalla para prescribir pruebas diagnósticas.
 */
public class PantallaPrescribirPrueba extends JFrame {

    private final SolicitudesService servicio;
    private final Usuario medico;

    //Componentes del formulario
    private final JTextField pacienteId = new JTextField(20);
    private final JTextField medicoId = new JTextField(20);

    private final JComboBox<TipoPrueba> tipos =
            new JComboBox<>();

    private final JTextArea indicacion = new JTextArea(3, 20);
    private final JTextArea justificacion = new JTextArea(3, 20);

    private final JComboBox<Prioridad> prioridades =
            new JComboBox<>(Prioridad.values());

    private final JButton guardar =
            new JButton("Registrar prescripcion");

    private final JButton actualizar =
            new JButton("Actualizar tipos de prueba");

    private final JLabel estado = new JLabel(" ");

    //Constructor
    public PantallaPrescribirPrueba(
            SolicitudesService servicio,
            Usuario medico) {

        super("CU11 - Prescribir prueba diagnostica");

        this.servicio = servicio;
        this.medico = medico;

        if (!disponiblePara(medico)) {
            throw new IllegalArgumentException(
                    "La pantalla solo esta disponible para medicos");
        }

        //Crear el formulario
        JPanel formulario = new JPanel(
                new GridLayout(0, 2, 10, 10));

        formulario.setBorder(
                BorderFactory.createEmptyBorder(15, 15, 15, 15));

        formulario.add(new JLabel("ID del paciente *"));
        formulario.add(pacienteId);

        formulario.add(new JLabel("ID del medico *"));
        formulario.add(medicoId);

        formulario.add(new JLabel("Tipo de prueba *"));
        formulario.add(tipos);

        formulario.add(new JLabel("Indicación clínica *"));

        indicacion.setLineWrap(true);
        indicacion.setWrapStyleWord(true);
        formulario.add(new JScrollPane(indicacion));

        formulario.add(new JLabel("Justificación *"));

        justificacion.setLineWrap(true);
        justificacion.setWrapStyleWord(true);
        formulario.add(new JScrollPane(justificacion));

        formulario.add(new JLabel("Prioridad *"));
        formulario.add(prioridades);

        //Mostrar el nombre de los tipos de prueba
        tipos.setRenderer(new DefaultListCellRenderer() {

            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> lista,
                    Object valor,
                    int indice,
                    boolean seleccionado,
                    boolean foco) {

                super.getListCellRendererComponent(
                        lista, valor, indice, seleccionado, foco);

                if (valor instanceof TipoPrueba tipo) {
                    setText(tipo.getNombre()
                            + " (ID " + tipo.getId() + ")");
                }

                return this;
            }
        });

        //Botones
        JPanel botones = new JPanel();

        botones.add(actualizar);
        botones.add(guardar);

        guardar.setEnabled(false);

        actualizar.addActionListener(
                e -> cargarTipos());

        guardar.addActionListener(
                e -> registrarSolicitud());

        //Organizar la ventana
        JPanel principal = new JPanel(new BorderLayout(10, 10));

        principal.add(
                new JLabel("Prescripción de pruebas diagnósticas"),
                BorderLayout.NORTH);

        principal.add(formulario, BorderLayout.CENTER);

        JPanel inferior = new JPanel(new BorderLayout());
        inferior.add(botones, BorderLayout.CENTER);
        inferior.add(estado, BorderLayout.SOUTH);

        principal.add(inferior, BorderLayout.SOUTH);

        add(principal);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(650, 480);
        setLocationRelativeTo(null);

        //Cargar los tipos al abrir la ventana
        cargarTipos();
    }

    //Consulta los tipos disponibles en el servidor
    private void cargarTipos() {

        actualizar.setEnabled(false);
        guardar.setEnabled(false);

        estado.setText("Cargando tipos de prueba...");

        SwingWorker<List<TipoPrueba>, Void> trabajador =
                new SwingWorker<>() {

            @Override
            protected List<TipoPrueba> doInBackground() {
                return servicio.consultarTiposPrueba(medico);
            }

            @Override
            protected void done() {

                actualizar.setEnabled(true);
                tipos.removeAllItems();

                try {
                    List<TipoPrueba> disponibles = get();

                    if (disponibles != null) {
                        for (TipoPrueba tipo : disponibles) {
                            if (tipo.isActivo()) {
                                tipos.addItem(tipo);
                            }
                        }
                    }

                    boolean hayTipos = tipos.getItemCount() > 0;

                    guardar.setEnabled(hayTipos);

                    estado.setText(hayTipos
                            ? "Tipos de prueba cargados correctamente"
                            : "No hay tipos de prueba disponibles");

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    error("La carga se ha interrumpido");

                } catch (ExecutionException ex) {
                    estado.setText("Error al cargar las pruebas");
                    error(mensajeError(ex.getCause()));
                }
            }
        };

        trabajador.execute();
    }

    //Comprueba el formulario y registra la solicitud
    private void registrarSolicitud() {

        long idPaciente;
        long idMedico;

        try {
            idPaciente = leerId(
                    pacienteId, "paciente");

            idMedico = leerId(
                    medicoId, "medico");

        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
            return;
        }

        TipoPrueba tipo = (TipoPrueba) tipos.getSelectedItem();

        String textoIndicacion =
                indicacion.getText().trim();

        String textoJustificacion =
                justificacion.getText().trim();

        Prioridad prioridad =
                (Prioridad) prioridades.getSelectedItem();

        if (tipo == null) {
            error("Selecciona un tipo de prueba");
            return;
        }

        if (textoIndicacion.isEmpty()) {
            error("La indicacion clinica es obligatoria");
            return;
        }

        if (textoJustificacion.isEmpty()) {
            error("La justificacion es obligatoria");
            return;
        }

        if (prioridad == null) {
            error("Selecciona una prioridad");
            return;
        }

        // Preparar la solicitud. El servidor asigna id, fecha y estado inicial.
        SolicitudPrueba datos = new SolicitudPrueba(
                0,
                idPaciente,
                idMedico,
                tipo.getId(),
                textoIndicacion,
                textoJustificacion,
                prioridad,
                null,
                null
        );

        guardar.setEnabled(false);
        estado.setText("Registrando solicitud...");

        //Enviar sin bloquear la ventana Swing
        SwingWorker<SolicitudPrueba, Void> trabajador =
                new SwingWorker<>() {

            @Override
            protected SolicitudPrueba doInBackground() {
                return servicio.crearSolicitud(medico, datos);
            }

            @Override
            protected void done() {

                guardar.setEnabled(tipos.getItemCount() > 0);

                try {
                    SolicitudPrueba creada = get();

                    estado.setText(
                            "Solicitud registrada correctamente");

                    JOptionPane.showMessageDialog(
                            PantallaPrescribirPrueba.this,
                            "Solicitud registrada correctamente"
                            + "\n\nID: " + creada.getId()
                            + "\nPaciente: " + creada.getPacienteId()
                            + "\nPrioridad: " + creada.getPrioridad()
                            + "\nEstado: " + creada.getEstado(),
                            "Prescripcion registrada",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    limpiarFormulario();

                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    error("La operacion se ha interrumpido");

                } catch (ExecutionException ex) {
                    estado.setText("No se ha registrado la solicitud");
                    error(mensajeError(ex.getCause()));
                }
            }
        };

        trabajador.execute();
    }

    //Comprueba que un identificador sea positivo.
    private long leerId(
            JTextField campo,
            String nombre) {

        try {
            long id = Long.parseLong(
                    campo.getText().trim());

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;

        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "El ID del " + nombre
                    + " debe ser un numero positivo");
        }
    }

    private void limpiarFormulario() {

        pacienteId.setText("");
        indicacion.setText("");
        justificacion.setText("");
        prioridades.setSelectedItem(Prioridad.ORDINARIA);
    }

    private static String mensajeError(Throwable causa) {

        if (causa == null || causa.getMessage() == null) {
            return "Error desconocido";
        }

        return causa.getMessage();
    }


private void error(String mensaje) {

    estado.setText("No se ha registrado la solicitud");

    JOptionPane.showMessageDialog(
            this,
            mensaje,
            "Error de prescripcion",
            JOptionPane.ERROR_MESSAGE);
}

    //Sólo los medicos pueden utilizar esta pantalla.
    public static boolean disponiblePara(Usuario usuario) {

        return usuario != null
                && usuario.rol() == Rol.MEDICO;
    }
    
    //Inicia la aplicacion. Argumentos opcionales: ROL URL.
    public static void main(String[] args) {

        Rol rol = args.length > 0
                ? Rol.valueOf(args[0])
                : Rol.MEDICO;

        String url = args.length > 1
                ? args[1]
                : "http://localhost:8080";

        Usuario usuario = new Usuario(
                System.getProperty("user.name"),
                rol);

        if (!disponiblePara(usuario)) {

            JOptionPane.showMessageDialog(
                    null,
                    "Solo el rol Medico puede prescribir pruebas",
                    "Sin permiso",
                    JOptionPane.ERROR_MESSAGE);

            return;
        }

        SolicitudesService servicio =
                new SolicitudesServiceHttp(url);

        SwingUtilities.invokeLater(() ->
                new PantallaPrescribirPrueba(
                        servicio, usuario).setVisible(true));
    }
}
