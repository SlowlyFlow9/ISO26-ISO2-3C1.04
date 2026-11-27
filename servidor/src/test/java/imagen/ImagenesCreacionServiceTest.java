package imagen;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;

import solicitud.EstadoSolicitud;
import solicitud.Prioridad;
import solicitud.SolicitudPrueba;
import solicitud.SolicitudesRepositorioMemoria;

class ImagenesCreacionServiceTest {

    private static final Usuario MEDICO =
            new Usuario("medico", Rol.MEDICO, 1L);

    private ImagenesRepositorioMemoria imagenes;
    private SolicitudesRepositorioMemoria solicitudes;
    private ImagenesService servicio;

    private String contenido;

    @BeforeEach
    void preparar() throws Exception {

        imagenes = new ImagenesRepositorioMemoria();

        solicitudes = new SolicitudesRepositorioMemoria();

        servicio = new ImagenesServiceImpl(
                imagenes, solicitudes);

        solicitudes.guardar(new SolicitudPrueba(
                1,
                10L,
                1L,
                20L,
                "Indicación clínica",
                "Justificación",
                Prioridad.ORDINARIA,
                LocalDateTime.of(2026, 11, 27, 9, 0),
                EstadoSolicitud.REALIZADA));

        contenido = ValidadorImagenTest.crearImagen("png");
    }

    private DatosCreacionImagen datosValidos() {

        return new DatosCreacionImagen(
                1,
                "radiografia.png",
                "image/png",
                contenido);
    }

    @Test
    @DisplayName("El médico puede registrar una imagen válida")
    void crearImagen() throws Exception {

        LocalDateTime antes = LocalDateTime.now();

        ResumenImagenPrueba creada =
                servicio.crear(MEDICO, datosValidos());

        LocalDateTime despues = LocalDateTime.now();

        assertAll(
                () -> assertEquals(1L, creada.id()),
                () -> assertEquals(1L, creada.solicitudId()),
                () -> assertEquals(1L, creada.medicoId()),
                () -> assertEquals(
                        "radiografia.png", creada.nombre()),
                () -> assertEquals(
                        "image/png", creada.tipoContenido()),
                () -> assertFalse(
                        creada.fechaCreacion().isBefore(antes)),
                () -> assertFalse(
                        creada.fechaCreacion().isAfter(despues)),
                () -> assertTrue(
                        imagenes.buscarPorId(creada.id())
                                .isPresent())
        );
    }

    @Test
    @DisplayName("Las imágenes creadas tienen identificadores distintos")
    void identificadoresDistintos() throws Exception {

        var primera =
                servicio.crear(MEDICO, datosValidos());

        var segunda =
                servicio.crear(MEDICO, datosValidos());

        assertNotEquals(primera.id(), segunda.id());

        assertEquals(
                2,
                servicio.listar(MEDICO, 1).size());
    }

    @Test
    @DisplayName("Una imagen creada se puede consultar mediante CU17")
    void crearYConsultar() throws Exception {

        var creada =
                servicio.crear(MEDICO, datosValidos());

        var recuperada =
                servicio.consultar(MEDICO, creada.id());

        assertAll(
                () -> assertEquals(
                        creada.id(), recuperada.id()),
                () -> assertEquals(
                        contenido, recuperada.contenido()),
                () -> assertEquals(
                        creada, recuperada.resumen()),
                () -> assertEquals(
                        java.util.List.of(creada),
                        servicio.listar(MEDICO, 1))
        );
    }

    @Test
    @DisplayName("No se puede crear una imagen de una solicitud inexistente")
    void solicitudInexistente() {

        var datos = new DatosCreacionImagen(
                999,
                "radiografia.png",
                "image/png",
                contenido);

        assertThrows(
                SolicitudImagenNoEncontradaException.class,
                () -> servicio.crear(MEDICO, datos));

        assertTrue(
                imagenes.listarPorSolicitud(999).isEmpty());
    }

    @Test
    @DisplayName("Los datos inválidos no modifican el repositorio")
    void datosInvalidos() {

        var datos = new DatosCreacionImagen(
                1,
                "radiografia.png",
                "image/png",
                "%%%");

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> servicio.crear(MEDICO, datos));

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }

    @Test
    @DisplayName("Los datos nulos no permiten crear una imagen")
    void datosNulos() {

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> servicio.crear(MEDICO, null));

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(
            value = Rol.class,
            names = {"MEDICO"},
            mode = EnumSource.Mode.EXCLUDE)
    void usuarioSinPermiso(Rol rol) {

        Usuario usuario =
                new Usuario("usuario", rol, 1L);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(usuario, datosValidos()));

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }

    @Test
    @DisplayName("Un usuario nulo no puede registrar imágenes")
    void usuarioNulo() {

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(null, datosValidos()));

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }

    @Test
    @DisplayName("Un médico sin identificador no puede crear imágenes")
    void medicoSinIdentificar() {

        Usuario usuario =
                new Usuario("medico", Rol.MEDICO);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(usuario, datosValidos()));

        assertTrue(
                imagenes.listarPorSolicitud(1).isEmpty());
    }

    @Test
    @DisplayName("Un médico con ID cero no puede crear imágenes")
    void medicoIdCero() {

        Usuario usuario =
                new Usuario("medico", Rol.MEDICO, 0L);

        assertThrows(
                SinPermisoException.class,
                () -> servicio.crear(usuario, datosValidos()));
    }

    @Test
    @DisplayName("El médico responsable es el usuario solicitante")
    void medicoResponsable() throws Exception {

        Usuario otroMedico =
                new Usuario("otroMedico", Rol.MEDICO, 25L);

        var creada =
                servicio.crear(otroMedico, datosValidos());

        assertEquals(25L, creada.medicoId());

        assertEquals(
                25L,
                servicio.consultar(MEDICO, creada.id())
                        .medicoId());
    }
}