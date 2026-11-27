package imagen;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Component;
import java.awt.Container;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import paciente.Rol;
import paciente.Usuario;
import paciente.SinPermisoException;

class PantallaConsultarImagenesTest {

    private static final Usuario MEDICO =
            new Usuario("medico", Rol.MEDICO, 1L);

    private static final LocalDateTime FECHA =
            LocalDateTime.of(2026, 11, 27, 10, 0);

    private PantallaConsultarImagenes pantalla;

    private JButton buscar;
    private JButton visualizar;
    private JTextField solicitud;
    private JTable tabla;
    private JLabel visor;
    private JLabel mensaje;

    private ServicioPruebas servicio;

    private static class ServicioPruebas
            implements ImagenesService {

        private List<ResumenImagenPrueba> imagenes =
                List.of(new ResumenImagenPrueba(
                        1,
                        1,
                        "radiografia.png",
                        "image/png",
                        1,
                        FECHA));

        private String contenido;

        private boolean fallarListado;
        private boolean fallarConsulta;

        private long solicitudConsultada;
        private long imagenConsultada;

        @Override
        public List<ResumenImagenPrueba> listar(
                Usuario usuario,
                long solicitudId)
                throws SinPermisoException {

            solicitudConsultada = solicitudId;

            if (fallarListado) {

                throw new SinPermisoException(
                        "Acceso denegado");
            }

            return imagenes;
        }

        @Override
        public ImagenPrueba consultar(
                Usuario usuario,
                long imagenId)
                throws ImagenNoEncontradaException {

            imagenConsultada = imagenId;

            if (fallarConsulta) {

                throw new ImagenNoEncontradaException(
                        "Imagen inexistente");
            }

            return new ImagenPrueba(
                    1,
                    1,
                    "radiografia.png",
                    "image/png",
                    contenido,
                    1,
                    FECHA);
        }
    }

    @BeforeEach
    void preparar() throws Exception {

        servicio = new ServicioPruebas();

        servicio.contenido =
                VisorImagenTest.crearImagenPrueba();

        enEdt(() -> {

            pantalla = new PantallaConsultarImagenes(
                    servicio, MEDICO);

            buscar = componente(
                    pantalla, "buscar", JButton.class);

            visualizar = componente(
                    pantalla, "visualizar", JButton.class);

            solicitud = componente(
                    pantalla, "solicitud", JTextField.class);

            tabla = componente(
                    pantalla, "tablaImagenes", JTable.class);

            visor = componente(
                    pantalla, "visorImagen", JLabel.class);

            mensaje = componente(
                    pantalla, "mensaje", JLabel.class);

            solicitud.setText("1");

            return null;
        });
    }

    @Test
    @DisplayName("La pantalla empieza sin imágenes y sin selección")
    void estadoInicial() throws Exception {

        enEdt(() -> {

            assertEquals(0, tabla.getRowCount());
            assertFalse(visualizar.isEnabled());
            assertNull(visor.getIcon());

            return null;
        });
    }

    @Test
    @DisplayName("Buscar una solicitud muestra sus imágenes")
    void buscarImagenes() throws Exception {

        ejecutarYEsperar(buscar);

        enEdt(() -> {

            assertEquals(1L, servicio.solicitudConsultada);
            assertEquals(1, tabla.getRowCount());

            assertEquals(
                    "radiografia.png",
                    tabla.getValueAt(0, 1));

            assertTrue(mensaje.getText()
                    .contains("1 imagen"));

            assertFalse(tabla.getModel()
                    .isCellEditable(0, 0));

            return null;
        });
    }

    @Test
    @DisplayName("Una solicitud sin imágenes muestra una tabla vacía")
    void listadoVacio() throws Exception {

        servicio.imagenes = List.of();

        ejecutarYEsperar(buscar);

        enEdt(() -> {

            assertEquals(0, tabla.getRowCount());

            assertTrue(mensaje.getText()
                    .contains("0 imagen"));

            return null;
        });
    }

    @Test
    @DisplayName("Seleccionar una imagen permite visualizarla")
    void visualizarImagen() throws Exception {

        ejecutarYEsperar(buscar);

        enEdt(() -> {

            tabla.setRowSelectionInterval(0, 0);

            assertTrue(visualizar.isEnabled());

            return null;
        });

        ejecutarYEsperar(visualizar);

        enEdt(() -> {

            assertEquals(1L, servicio.imagenConsultada);
            assertNotNull(visor.getIcon());

            assertEquals(
                    20, visor.getIcon().getIconWidth());

            assertEquals(
                    10, visor.getIcon().getIconHeight());

            assertEquals(
                    "Imagen cargada correctamente",
                    mensaje.getText());

            return null;
        });
    }

    @Test
    @DisplayName("Un identificador no numérico no realiza la búsqueda")
    void idNoNumerico() throws Exception {

        enEdt(() -> {

            solicitud.setText("abc");
            buscar.doClick(0);

            assertTrue(mensaje.getText()
                    .contains("ID de solicitud positivo"));

            assertEquals(0L, servicio.solicitudConsultada);

            return null;
        });
    }

    @Test
    @DisplayName("Un identificador igual a cero es rechazado")
    void idCero() throws Exception {

        enEdt(() -> {

            solicitud.setText("0");
            buscar.doClick(0);

            assertTrue(mensaje.getText()
                    .contains("ID de solicitud positivo"));

            assertEquals(0L, servicio.solicitudConsultada);

            return null;
        });
    }

    @Test
    @DisplayName("Los errores del listado se muestran al médico")
    void errorListado() throws Exception {

        servicio.fallarListado = true;

        ejecutarYEsperar(buscar);

        enEdt(() -> {

            assertEquals(0, tabla.getRowCount());

            assertTrue(mensaje.getText()
                    .contains("Acceso denegado"));

            assertTrue(buscar.isEnabled());

            return null;
        });
    }

    @Test
    @DisplayName("Los errores al consultar una imagen se muestran")
    void errorConsulta() throws Exception {

        servicio.fallarConsulta = true;

        ejecutarYEsperar(buscar);

        enEdt(() -> {

            tabla.setRowSelectionInterval(0, 0);

            return null;
        });

        ejecutarYEsperar(visualizar);

        enEdt(() -> {

            assertNull(visor.getIcon());

            assertTrue(mensaje.getText()
                    .contains("Imagen inexistente"));

            return null;
        });
    }

    @Test
    @DisplayName("Una imagen incorrecta no se puede visualizar")
    void imagenCorrupta() throws Exception {

        servicio.contenido = "%%%";

        ejecutarYEsperar(buscar);

        enEdt(() -> {

            tabla.setRowSelectionInterval(0, 0);

            return null;
        });

        ejecutarYEsperar(visualizar);

        enEdt(() -> {

            assertNull(visor.getIcon());

            assertTrue(mensaje.getText()
                    .contains("No se pudo visualizar"));

            return null;
        });
    }

    @Test
    @DisplayName("Una nueva búsqueda elimina la imagen anterior")
    void nuevaBusqueda() throws Exception {

        ejecutarYEsperar(buscar);

        enEdt(() -> {

            tabla.setRowSelectionInterval(0, 0);

            return null;
        });

        ejecutarYEsperar(visualizar);

        enEdt(() -> {

            assertNotNull(visor.getIcon());

            return null;
        });

        servicio.imagenes = List.of();

        ejecutarYEsperar(buscar);

        enEdt(() -> {

            assertNull(visor.getIcon());
            assertEquals(0, tabla.getRowCount());
            assertFalse(visualizar.isEnabled());

            return null;
        });
    }

    @Test
    @DisplayName("La pantalla no está disponible para otros roles")
    void controlPermisos() throws Exception {

        Usuario paciente =
                new Usuario("paciente", Rol.PACIENTE);

        assertTrue(
                PantallaConsultarImagenes.disponiblePara(MEDICO));

        assertFalse(
                PantallaConsultarImagenes.disponiblePara(paciente));

        assertFalse(
                PantallaConsultarImagenes.disponiblePara(null));

        enEdt(() -> {

            assertThrows(
                    IllegalArgumentException.class,
                    () -> new PantallaConsultarImagenes(
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
                    () -> new PantallaConsultarImagenes(
                            null, MEDICO));

            return null;
        });
    }

    private void ejecutarYEsperar(
            JButton boton) throws Exception {

        CountDownLatch terminado =
                new CountDownLatch(1);

        java.beans.PropertyChangeListener escucha = e -> {

            if (Boolean.TRUE.equals(e.getNewValue())) {
                terminado.countDown();
            }
        };

        enEdt(() -> {

            buscar.addPropertyChangeListener(
                    "enabled", escucha);

            boton.doClick(0);

            return null;
        });

        try {

            assertTrue(
                    terminado.await(5, TimeUnit.SECONDS),
                    "La operación no ha terminado");

        } finally {

            enEdt(() -> {

                buscar.removePropertyChangeListener(
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