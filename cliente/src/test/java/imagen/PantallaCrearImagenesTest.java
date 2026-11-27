package imagen;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import paciente.Rol;
import paciente.Usuario;

class PantallaCrearImagenesTest {

    private static final Usuario MEDICO =
            new Usuario("medico", Rol.MEDICO, 1L);

    @TempDir
    Path carpeta;

    private Path archivo;

    private PantallaCrearImagenes pantalla;

    private JTextField solicitud;
    private JButton seleccionar;
    private JButton registrar;
    private JLabel nombreArchivo;
    private JLabel visor;
    private JLabel mensaje;

    private ServicioPruebas servicio;

    private static class ServicioPruebas
            implements ImagenesCreacionService {

        private DatosCreacionImagen datosRecibidos;
        private Usuario usuarioRecibido;
        private boolean fallar;
        private int peticiones;

        @Override
        public ResumenImagenPrueba crear(
                Usuario usuario,
                DatosCreacionImagen datos)
                throws SolicitudImagenNoEncontradaException {

            peticiones++;

            datosRecibidos = datos;
            usuarioRecibido = usuario;

            if (fallar) {

                throw new SolicitudImagenNoEncontradaException(
                        "Solicitud inexistente");
            }

            return new ResumenImagenPrueba(
                    15,
                    datos.solicitudId(),
                    datos.nombre(),
                    datos.tipoContenido(),
                    usuario.medicoId(),
                    LocalDateTime.of(2026, 11, 28, 10, 0));
        }
    }

    @BeforeEach
    void preparar() throws Exception {

        servicio = new ServicioPruebas();

        archivo = CargadorImagenTest.crearImagen(
                carpeta, "png", 20, 10);

        enEdt(() -> {

            pantalla = new PantallaCrearImagenes(
                    servicio, MEDICO);

            solicitud = componente(
                    pantalla, "solicitud", JTextField.class);

            seleccionar = componente(
                    pantalla, "seleccionar", JButton.class);

            registrar = componente(
                    pantalla, "registrar", JButton.class);

            nombreArchivo = componente(
                    pantalla, "nombreArchivo", JLabel.class);

            visor = componente(
                    pantalla, "visorImagen", JLabel.class);

            mensaje = componente(
                    pantalla, "mensaje", JLabel.class);

            solicitud.setText("1");

            return null;
        });
    }

    @Test
    @DisplayName("La pantalla empieza sin ningún archivo seleccionado")
    void estadoInicial() throws Exception {

        enEdt(() -> {

            assertFalse(registrar.isEnabled());

            assertTrue(seleccionar.isEnabled());

            assertNull(visor.getIcon());

            assertEquals(
                    "Ningún archivo seleccionado",
                    nombreArchivo.getText());

            return null;
        });
    }

    @Test
    @DisplayName("Seleccionar una imagen permite previsualizarla")
    void seleccionarImagen() throws Exception {

        ejecutarYEsperar(
                () -> pantalla.seleccionarArchivo(archivo));

        enEdt(() -> {

            assertEquals(
                    "imagen.png",
                    nombreArchivo.getText());

            assertNotNull(visor.getIcon());

            assertEquals(
                    20, visor.getIcon().getIconWidth());

            assertEquals(
                    10, visor.getIcon().getIconHeight());

            assertTrue(registrar.isEnabled());

            assertEquals(0, servicio.peticiones);

            return null;
        });
    }

    @Test
    @DisplayName("Una imagen incorrecta no habilita el registro")
    void seleccionarImagenInvalida() throws Exception {

        Path invalida = Files.writeString(
                carpeta.resolve("falso.png"), "Texto");

        ejecutarYEsperar(
                () -> pantalla.seleccionarArchivo(invalida));

        enEdt(() -> {

            assertNull(visor.getIcon());

            assertFalse(registrar.isEnabled());

            assertTrue(mensaje.getText().contains("Error"));

            assertEquals(0, servicio.peticiones);

            return null;
        });
    }

    @Test
    @DisplayName("El registro envía la imagen y la solicitud al servicio")
    void registrarImagen() throws Exception {

        ejecutarYEsperar(
                () -> pantalla.seleccionarArchivo(archivo));

        ejecutarYEsperar(
                () -> registrar.doClick(0));

        enEdt(() -> {

            assertEquals(1, servicio.peticiones);

            assertEquals(
                    MEDICO, servicio.usuarioRecibido);

            assertEquals(
                    1L,
                    servicio.datosRecibidos.solicitudId());

            assertEquals(
                    "imagen.png",
                    servicio.datosRecibidos.nombre());

            assertEquals(
                    "image/png",
                    servicio.datosRecibidos.tipoContenido());

            assertFalse(
                    servicio.datosRecibidos.contenido().isBlank());

            assertTrue(mensaje.getText().contains("ID: 15"));

            assertFalse(registrar.isEnabled());

            assertNull(visor.getIcon());

            assertEquals(
                    "Ningún archivo seleccionado",
                    nombreArchivo.getText());

            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "0", "-1"})
    void solicitudInvalida(String id) throws Exception {

        ejecutarYEsperar(
                () -> pantalla.seleccionarArchivo(archivo));

        enEdt(() -> {

            solicitud.setText(id);

            registrar.doClick(0);

            assertTrue(mensaje.getText()
                    .contains("ID de solicitud positivo"));

            assertEquals(0, servicio.peticiones);

            assertTrue(registrar.isEnabled());

            assertNotNull(visor.getIcon());

            return null;
        });
    }

    @Test
    @DisplayName("Un error del servidor conserva la imagen seleccionada")
    void errorRegistro() throws Exception {

        servicio.fallar = true;

        ejecutarYEsperar(
                () -> pantalla.seleccionarArchivo(archivo));

        ejecutarYEsperar(
                () -> registrar.doClick(0));

        enEdt(() -> {

            assertEquals(1, servicio.peticiones);

            assertTrue(mensaje.getText()
                    .contains("Solicitud inexistente"));

            assertNotNull(visor.getIcon());

            assertTrue(registrar.isEnabled());

            return null;
        });
    }

    @Test
    @DisplayName("Se puede reintentar un registro que ha fallado")
    void reintentarRegistro() throws Exception {

        servicio.fallar = true;

        ejecutarYEsperar(
                () -> pantalla.seleccionarArchivo(archivo));

        ejecutarYEsperar(
                () -> registrar.doClick(0));

        servicio.fallar = false;

        enEdt(() -> {

            solicitud.setText("2");

            return null;
        });

        ejecutarYEsperar(
                () -> registrar.doClick(0));

        enEdt(() -> {

            assertEquals(2, servicio.peticiones);

            assertEquals(
                    2L,
                    servicio.datosRecibidos.solicitudId());

            assertTrue(
                    mensaje.getText().contains("ID: 15"));

            assertFalse(registrar.isEnabled());

            return null;
        });
    }

    @Test
    @DisplayName("Una selección incorrecta elimina la imagen anterior")
    void nuevaSeleccionInvalida() throws Exception {

        ejecutarYEsperar(
                () -> pantalla.seleccionarArchivo(archivo));

        Path inexistente =
                carpeta.resolve("inexistente.png");

        ejecutarYEsperar(
                () -> pantalla.seleccionarArchivo(inexistente));

        enEdt(() -> {

            assertNull(visor.getIcon());

            assertFalse(registrar.isEnabled());

            assertTrue(mensaje.getText().contains("Error"));

            return null;
        });
    }

    @Test
    @DisplayName("Solo los médicos identificados pueden acceder")
    void controlPermisos() throws Exception {

        Usuario paciente =
                new Usuario("paciente", Rol.PACIENTE);

        Usuario medicoSinId =
                new Usuario("medico", Rol.MEDICO);

        Usuario medicoIdCero =
                new Usuario("medico", Rol.MEDICO, 0L);

        assertTrue(
                PantallaCrearImagenes.disponiblePara(MEDICO));

        assertFalse(
                PantallaCrearImagenes.disponiblePara(paciente));

        assertFalse(
                PantallaCrearImagenes.disponiblePara(medicoSinId));

        assertFalse(
                PantallaCrearImagenes.disponiblePara(medicoIdCero));

        assertFalse(
                PantallaCrearImagenes.disponiblePara(null));

        enEdt(() -> {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> new PantallaCrearImagenes(
                            servicio, paciente));

            return null;
        });
    }

    @Test
    @DisplayName("El servicio es obligatorio para construir la pantalla")
    void servicioNulo() throws Exception {

        enEdt(() -> {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> new PantallaCrearImagenes(
                            null, MEDICO));

            return null;
        });
    }

    private void ejecutarYEsperar(
            Runnable accion) throws Exception {

        CountDownLatch terminado =
                new CountDownLatch(1);

        java.beans.PropertyChangeListener escucha = e -> {

            if (Boolean.TRUE.equals(e.getNewValue())) {

                terminado.countDown();
            }
        };

        enEdt(() -> {

            seleccionar.addPropertyChangeListener(
                    "enabled", escucha);

            accion.run();

            return null;
        });

        try {

            assertTrue(
                    terminado.await(5, TimeUnit.SECONDS),
                    "La operación no ha terminado");

        } finally {

            enEdt(() -> {

                seleccionar.removePropertyChangeListener(
                        "enabled", escucha);

                return null;
            });
        }
    }

    private static <T> T enEdt(
            Callable<T> tarea) throws Exception {

        FutureTask<T> trabajo =
                new FutureTask<>(tarea);

        SwingUtilities.invokeAndWait(trabajo);

        return trabajo.get();
    }

    private static <T extends Component> T componente(
            Container contenedor,
            String nombre,
            Class<T> tipo) {

        for (Component elemento
                : contenedor.getComponents()) {

            if (nombre.equals(elemento.getName())
                    && tipo.isInstance(elemento)) {

                return tipo.cast(elemento);
            }

            if (elemento instanceof Container hijo) {

                T resultado =
                        componente(hijo, nombre, tipo);

                if (resultado != null) {

                    return resultado;
                }
            }
        }

        return null;
    }
}