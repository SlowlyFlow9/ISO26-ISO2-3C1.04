package imagen;

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