package imagen;

import java.time.LocalDateTime;
import java.util.List;

import paciente.ControlAcceso;
import paciente.Rol;
import paciente.SinPermisoException;
import paciente.Usuario;
import solicitud.SolicitudesRepositorio;

public class ImagenesServiceImpl
        implements ImagenesService {

    private final ImagenesRepositorio imagenes;
    private final SolicitudesRepositorio solicitudes;

    public ImagenesServiceImpl(
            ImagenesRepositorio imagenes,
            SolicitudesRepositorio solicitudes) {

        this.imagenes = imagenes;
        this.solicitudes = solicitudes;
    }

    // CU17 - Consultar listado de imágenes.
    @Override
    public List<ResumenImagenPrueba> listar(
            Usuario solicitante,
            long solicitudId)
            throws SinPermisoException,
            SolicitudImagenNoEncontradaException {

        ControlAcceso.exigir(solicitante, Rol.MEDICO);

        validarId(solicitudId);
        comprobarSolicitud(solicitudId);

        return imagenes.listarPorSolicitud(solicitudId)
                .stream()
                .map(ImagenPrueba::resumen)
                .toList();
    }

    // CU17 - Consultar una imagen.
    @Override
    public ImagenPrueba consultar(
            Usuario solicitante,
            long imagenId)
            throws SinPermisoException,
            ImagenNoEncontradaException,
            SolicitudImagenNoEncontradaException {

        ControlAcceso.exigir(solicitante, Rol.MEDICO);

        validarId(imagenId);

        ImagenPrueba imagen =
                imagenes.buscarPorId(imagenId)
                        .orElseThrow(() ->
                                new ImagenNoEncontradaException(
                                        "No existe la imagen con ID "
                                                + imagenId));

        comprobarSolicitud(imagen.solicitudId());

        return imagen;
    }

    // CU18 - Registrar una nueva imagen.
    @Override
    public ResumenImagenPrueba crear(
            Usuario solicitante,
            DatosCreacionImagen datos)
            throws SinPermisoException,
            SolicitudImagenNoEncontradaException,
            DatosImagenInvalidosException {

        ControlAcceso.exigir(solicitante, Rol.MEDICO);

        if (solicitante.medicoId() == null
                || solicitante.medicoId() <= 0) {

            throw new SinPermisoException(
                    "El médico debe estar identificado");
        }

        ValidadorImagen.validar(datos);

        comprobarSolicitud(datos.solicitudId());

        ImagenPrueba imagen = new ImagenPrueba(
                0,
                datos.solicitudId(),
                datos.nombre().trim(),
                datos.tipoContenido(),
                datos.contenido(),
                solicitante.medicoId(),
                LocalDateTime.now());

        return imagenes.guardar(imagen).resumen();
    }

    private void comprobarSolicitud(long solicitudId)
            throws SolicitudImagenNoEncontradaException {

        if (solicitudes.buscarPorId(solicitudId).isEmpty()) {

            throw new SolicitudImagenNoEncontradaException(
                    "No existe la solicitud con ID "
                            + solicitudId);
        }
    }

    private static void validarId(long id) {

        if (id <= 0) {

            throw new IllegalArgumentException(
                    "El identificador debe ser positivo");
        }
    }
}