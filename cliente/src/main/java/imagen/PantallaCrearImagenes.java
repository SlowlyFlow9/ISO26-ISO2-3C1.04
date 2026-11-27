package imagen;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import paciente.Rol;
import paciente.Usuario;

public class PantallaCrearImagenes extends JPanel {

    private final ImagenesCreacionService servicio;
    private final Usuario usuario;

    private ArchivoImagen archivo;

    private final JTextField solicitud =
            new JTextField(10);

    private final JButton seleccionar =
            new JButton("Seleccionar archivo");

    private final JButton registrar =
            new JButton("Registrar imagen");

    private final JLabel nombreArchivo =
            new JLabel("Ningún archivo seleccionado");

    private final JLabel visor =
            new JLabel(
                    "Selecciona una imagen",
                    SwingConstants.CENTER);

    private final JLabel mensaje =
            new JLabel("Introduce el ID de la solicitud");

    public PantallaCrearImagenes(
            ImagenesCreacionService servicio,
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

        setLayout(new BorderLayout(10, 10));

        solicitud.setName("solicitud");
        seleccionar.setName("seleccionar");
        registrar.setName("registrar");
        nombreArchivo.setName("nombreArchivo");
        visor.setName("visorImagen");
        mensaje.setName("mensaje");

        JPanel formulario = new JPanel();

        formulario.add(new JLabel("ID de solicitud:"));
        formulario.add(solicitud);
        formulario.add(seleccionar);
        formulario.add(nombreArchivo);

        add(formulario, BorderLayout.NORTH);

        JScrollPane panelVisor =
                new JScrollPane(visor);

        panelVisor.setPreferredSize(
                new Dimension(600, 400));

        add(panelVisor, BorderLayout.CENTER);

        JPanel botones = new JPanel();

        botones.add(registrar);

        JPanel inferior =
                new JPanel(new BorderLayout());

        inferior.add(botones, BorderLayout.CENTER);
        inferior.add(mensaje, BorderLayout.SOUTH);

        add(inferior, BorderLayout.SOUTH);

        registrar.setEnabled(false);

        seleccionar.addActionListener(e -> elegirArchivo());

        registrar.addActionListener(e -> registrarImagen());
    }

    private void elegirArchivo() {

        JFileChooser selector = new JFileChooser();

        selector.setDialogTitle(
                "Seleccionar imagen de prueba");

        selector.setAcceptAllFileFilterUsed(false);

        selector.setFileFilter(
                new FileNameExtensionFilter(
                        "Imágenes PNG y JPEG",
                        "png", "jpg", "jpeg"));

        if (selector.showOpenDialog(this)
                == JFileChooser.APPROVE_OPTION) {

            seleccionarArchivo(
                    selector.getSelectedFile().toPath());
        }
    }

    void seleccionarArchivo(Path ruta) {

        if (!seleccionar.isEnabled()) {

            return;
        }

        limpiarArchivo();

        mensaje.setText("Cargando archivo...");

        ejecutar(
                () -> CargadorImagen.cargar(ruta),
                resultado -> {

                    archivo = resultado;

                    nombreArchivo.setText(archivo.nombre());

                    visor.setText("");

                    visor.setIcon(
                            new ImageIcon(archivo.vistaPrevia()));

                    mensaje.setText(
                            "Archivo seleccionado correctamente");
                });
    }

    private void registrarImagen() {

        if (archivo == null) {

            return;
        }

        long solicitudId;

        try {

            solicitudId = Long.parseLong(
                    solicitud.getText().trim());

            if (solicitudId <= 0) {

                throw new IllegalArgumentException(
                        "ID incorrecto");
            }

        } catch (IllegalArgumentException e) {

            mensaje.setText(
                    "Introduce un ID de solicitud positivo");

            return;
        }

        DatosCreacionImagen datos =
                archivo.datos(solicitudId);

        mensaje.setText("Registrando imagen...");

        ejecutar(
                () -> servicio.crear(usuario, datos),
                imagen -> {

                    limpiarArchivo();

                    mensaje.setText(
                            "Imagen registrada correctamente. ID: "
                            + imagen.id());
                });
    }

    private void limpiarArchivo() {

        archivo = null;

        nombreArchivo.setText(
                "Ningún archivo seleccionado");

        visor.setIcon(null);

        visor.setText("Selecciona una imagen");

        registrar.setEnabled(false);
    }

    private void cambiarEstado(boolean ocupado) {

        solicitud.setEnabled(!ocupado);

        seleccionar.setEnabled(!ocupado);

        registrar.setEnabled(
                !ocupado && archivo != null);
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