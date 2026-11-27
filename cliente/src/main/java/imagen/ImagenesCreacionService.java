package imagen;

import paciente.SinPermisoException;
import paciente.Usuario;

public interface ImagenesCreacionService {

    ResumenImagenPrueba crear(
            Usuario solicitante,
            DatosCreacionImagen datos)
            throws SinPermisoException,
            SolicitudImagenNoEncontradaException,
            DatosImagenInvalidosException;
}