package imagen;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface ImagenesService {

    List<ResumenImagenPrueba> listar(
            Usuario solicitante,
            long solicitudId)
            throws SinPermisoException,
            SolicitudImagenNoEncontradaException;

    ImagenPrueba consultar(
            Usuario solicitante,
            long imagenId)
            throws SinPermisoException,
            ImagenNoEncontradaException,
            SolicitudImagenNoEncontradaException;
}