package imagen;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import paciente.Rol;
import paciente.Usuario;

public class PantallaConsultarImagenes extends JPanel {

    private final ImagenesService servicio;
    private final Usuario usuario;

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final JTextField solicitud =
            new JTextField(10);

    private final JButton buscar =
            new JButton("Buscar");

    private final JButton visualizar =
            new JButton("Visualizar imagen");

    private final JLabel mensaje =
            new JLabel("Introduce el ID de una solicitud");

    private final JLabel visor =
            new JLabel("Selecciona una imagen",
                    SwingConstants.CENTER);

    private final DefaultTableModel modelo =
            new DefaultTableModel(
                    new Object[]{
                            "ID",
                            "Nombre",
                            "Formato",
                            "Médico",
                            "Fecha"
                    }, 0) {

                @Override
                public boolean isCellEditable(
                        int fila, int columna) {

                    return false;
                }
            };

    private final JTable tabla =
            new JTable(modelo);

    public PantallaConsultarImagenes(
            ImagenesService servicio,
            Usuario usuario) {

        if (servicio == null) {

            throw new IllegalArgumentException(
                    "El servicio es obligatorio");
        }

        if (!disponiblePara(usuario)) {

            throw new IllegalArgumentException(
                    "La pantalla solo está disponible para médicos");
        }

        this.servicio = servicio;
        this.usuario = usuario;

        construir();
    }

    public static boolean disponiblePara(Usuario usuario) {

        return usuario != null
                && usuario.rol() == Rol.MEDICO;
    }

    private void construir() {

        setLayout(new BorderLayout(10, 10));

        solicitud.setName("solicitud");
        buscar.setName("buscar");
        visualizar.setName("visualizar");
        tabla.setName("tablaImagenes");
        visor.setName("visorImagen");
        mensaje.setName("mensaje");

        JPanel formulario = new JPanel();

        formulario.add(new JLabel("ID de solicitud:"));
        formulario.add(solicitud);
        formulario.add(buscar);

        add(formulario, BorderLayout.NORTH);

        tabla.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        tabla.getSelectionModel()
                .addListSelectionListener(e -> {

                    if (!e.getValueIsAdjusting()) {

                        visualizar.setEnabled(
                                buscar.isEnabled()
                                && tabla.getSelectedRow() >= 0);
                    }
                });

        visualizar.setEnabled(false);

        JPanel panelListado =
                new JPanel(new BorderLayout());

        panelListado.add(
                new JScrollPane(tabla),
                BorderLayout.CENTER);

        panelListado.add(
                visualizar,
                BorderLayout.SOUTH);

        JScrollPane panelVisor =
                new JScrollPane(visor);

        panelVisor.setPreferredSize(
                new Dimension(450, 400));

        JSplitPane separador =
                new JSplitPane(
                        JSplitPane.HORIZONTAL_SPLIT,
                        panelListado,
                        panelVisor);

        separador.setResizeWeight(0.5);

        add(separador, BorderLayout.CENTER);
        add(mensaje, BorderLayout.SOUTH);

        buscar.addActionListener(e -> buscarImagenes());

        solicitud.addActionListener(e -> {

            if (buscar.isEnabled()) {
                buscarImagenes();
            }
        });

        visualizar.addActionListener(e -> mostrarImagen());
    }

    private void limpiarVisor() {

        visor.setIcon(null);
        visor.setText("Selecciona una imagen");
    }

    private void buscarImagenes() {

        long id;

        try {

            id = Long.parseLong(
                    solicitud.getText().trim());

            if (id <= 0) {

                throw new IllegalArgumentException(
                        "Identificador no válido");
            }

        } catch (IllegalArgumentException e) {

            mensaje.setText(
                    "Introduce un ID de solicitud positivo");

            return;
        }

        modelo.setRowCount(0);
        limpiarVisor();

        mensaje.setText("Buscando imágenes...");

        ejecutar(
                () -> servicio.listar(usuario, id),
                this::mostrarListado);
    }

    private void mostrarListado(
            List<ResumenImagenPrueba> imagenes) {

        for (ResumenImagenPrueba imagen : imagenes) {

            modelo.addRow(new Object[]{
                    imagen.id(),
                    imagen.nombre(),
                    imagen.tipoContenido(),
                    imagen.medicoId(),
                    imagen.fechaCreacion()
                            .format(FORMATO_FECHA)
            });
        }

        mensaje.setText(
                imagenes.size() + " imagen(es) encontrada(s)");
    }

    private void mostrarImagen() {

        int fila = tabla.getSelectedRow();

        if (fila < 0) {
            return;
        }

        long id = ((Number)
                modelo.getValueAt(fila, 0)).longValue();

        limpiarVisor();

        mensaje.setText("Cargando imagen...");

        ejecutar(
                () -> {

                    ImagenPrueba imagen =
                            servicio.consultar(usuario, id);

                    return VisorImagen.decodificar(
                            imagen.contenido());
                },
                this::mostrarContenido);
    }

    private void mostrarContenido(
            BufferedImage imagen) {

        visor.setText("");
        visor.setIcon(new ImageIcon(imagen));

        mensaje.setText("Imagen cargada correctamente");
    }

    private void cambiarEstado(boolean ocupado) {

        buscar.setEnabled(!ocupado);
        solicitud.setEnabled(!ocupado);
        tabla.setEnabled(!ocupado);

        visualizar.setEnabled(
                !ocupado
                && tabla.getSelectedRow() >= 0);
    }

    private <T> void ejecutar(
            Callable<T> tarea,
            Consumer<T> resultado) {

        cambiarEstado(true);

        SwingWorker<T, Void> trabajador =
                new SwingWorker<>() {

                    @Override
                    protected T doInBackground()
                            throws Exception {

                        return tarea.call();
                    }

                    @Override
                    protected void done() {

                        try {

                            resultado.accept(get());

                        } catch (InterruptedException e) {

                            Thread.currentThread().interrupt();

                            mensaje.setText(
                                    "La operación fue interrumpida");

                        } catch (ExecutionException e) {

                            mensaje.setText(
                                    "Error: "
                                    + e.getCause().getMessage());

                        } finally {

                            cambiarEstado(false);
                        }
                    }
                };

        trabajador.execute();
    }
}