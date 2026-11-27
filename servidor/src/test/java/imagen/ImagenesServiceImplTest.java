package imagen;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;
import solicitud.SolicitudesRepositorioMemoria;

class ImagenesServiceImplTest {

    private static final Usuario MEDICO =
            new Usuario("medico", Rol.MEDICO, 1L);

    private ImagenesRepositorioMemoria imagenes;
    private SolicitudesRepositorioMemoria solicitudes;
    private ImagenesService servicio;

    private ImagenPrueba primera;
    private ImagenPrueba segunda;

    @BeforeEach
    void preparar() {

        imagenes = new ImagenesRepositorioMemoria();
        solicitudes = new SolicitudesRepositorioMemoria();

        servicio = new ImagenesServiceImpl(imagenes, solicitudes);

        solicitudes.guardar(solicitud(1));
        solicitudes.guardar(solicitud(2));
        solicitudes.guardar(solicitud(3));

        primera = imagenes.guardar(imagen(1));
        segunda = imagenes.guardar(imagen(1));

        imagenes.guardar(imagen(2));
    }

    private SolicitudPrueba solicitud(long id) {

        return new SolicitudPrueba(
                id,
                10L,
                1L,
                20L,
                "Indicación clínica",
                "Justificación",
                Prioridad.ORDINARIA,
                LocalDateTime.of(2026, 11, 27, 9, 0),
                EstadoSolicitud.REALIZADA);
    }

    private ImagenPrueba imagen(long solicitudId) {

        return new ImagenPrueba(
                0,
                solicitudId,
                "radiografia.png",
                "image/png",
                "aW1hZ2Vu",
                1L,
                LocalDateTime.of(2026, 11, 27, 10, 0));
    }

    @Test
    @DisplayName("El médico obtiene solo las imágenes de la solicitud")
    void listarImagenes() throws Exception {

        var resultado = servicio.listar(MEDICO, 1);

        assertEquals(
                java.util.List.of(
                        primera.resumen(),
                        segunda.resumen()),
                resultado);
    }

    @Test
    @DisplayName("Una solicitud sin imágenes devuelve un listado vacío")
    void solicitudSinImagenes() throws Exception {

        assertTrue(servicio.listar(MEDICO, 3).isEmpty());
    }

    @Test
    @DisplayName("Consultar imágenes de una solicitud inexistente falla")
    void solicitudInexistente() {

        assertThrows(
                SolicitudImagenNoEncontradaException.class,
                () -> servicio.listar(MEDICO, 999));
    }

    @Test
    @DisplayName("El médico puede recuperar el contenido de una imagen")
    void consultarImagen() throws Exception {

        ImagenPrueba resultado =
                servicio.consultar(MEDICO, primera.id());

        assertAll(
                () -> assertEquals(primera.id(), resultado.id()),
                () -> assertEquals(1L, resultado.solicitudId()),
                () -> assertEquals(
                        "radiografia.png", resultado.nombre()),
                () -> assertEquals(
                        "image/png", resultado.tipoContenido()),
                () -> assertEquals(
                        "aW1hZ2Vu", resultado.contenido())
        );
    }

    @Test
    @DisplayName("Consultar una imagen inexistente genera un error")
    void imagenInexistente() {

        assertThrows(
                ImagenNoEncontradaException.class,
                () -> servicio.consultar(MEDICO, 999));
    }

    @ParameterizedTest(name = "ID de solicitud inválido: {0}")
    @ValueSource(longs = {-1, 0})
    void solicitudIdInvalido(long id) {

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.listar(MEDICO, id));
    }

    @ParameterizedTest(name = "ID de imagen inválido: {0}")
    @ValueSource(longs = {-1, 0})
    void imagenIdInvalido(long id) {

        assertThrows(
                IllegalArgumentException.class,
                () -> servicio.consultar(MEDICO, id));
    }

    @ParameterizedTest(name = "El rol {0} no puede listar imágenes")
    @EnumSource(
            value = Rol.class,
            names = {"MEDICO"},
            mode = EnumSource.Mode.EXCLUDE)
    void listarSinPermiso(Rol rol) {

        Usuario usuario = new Usuario("usuario", rol);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.listar(usuario, 1));
    }

    @ParameterizedTest(name = "El rol {0} no puede consultar imágenes")
    @EnumSource(
            value = Rol.class,
            names = {"MEDICO"},
            mode = EnumSource.Mode.EXCLUDE)
    void consultarSinPermiso(Rol rol) {

        Usuario usuario = new Usuario("usuario", rol);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.consultar(usuario, primera.id()));
    }

    @Test
    @DisplayName("Un usuario nulo no puede listar imágenes")
    void listarUsuarioNulo() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.listar(null, 1));
    }

    @Test
    @DisplayName("Un usuario nulo no puede consultar imágenes")
    void consultarUsuarioNulo() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.consultar(null, primera.id()));
    }

    @Test
    @DisplayName("Una imagen sin solicitud existente no se puede consultar")
    void imagenSinSolicitud() {

        ImagenPrueba huerfana =
                imagenes.guardar(imagen(999));

        assertThrows(
                SolicitudImagenNoEncontradaException.class,
                () -> servicio.consultar(MEDICO, huerfana.id()));
    }
}