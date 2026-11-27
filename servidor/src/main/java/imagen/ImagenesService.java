package imagen;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface ImagenesService {

    // CU17
    List<ResumenImagenPrueba> listar(
            Usuario solicitante,
            long solicitudId)
            throws SinPermisoException,
            SolicitudImagenNoEncontradaException;

    // CU17
    ImagenPrueba consultar(
            Usuario solicitante,
            long imagenId)
            throws SinPermisoException,
            ImagenNoEncontradaException,
            SolicitudImagenNoEncontradaException;

    // CU18
    ResumenImagenPrueba crear(
            Usuario solicitante,
            DatosCreacionImagen datos)
            throws SinPermisoException,
            SolicitudImagenNoEncontradaException,
            DatosImagenInvalidosException;
}