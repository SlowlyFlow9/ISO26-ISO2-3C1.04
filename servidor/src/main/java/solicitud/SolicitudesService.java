package solicitud;

import java.util.List;
import java.util.Optional;

import paciente.Usuario;
import paciente.SinPermisoException;
import tipoprueba.TipoPrueba;

/**
 * Operaciones del servicio de solicitudes.
 * CU11 - Prescribir pruebas diagnosticas.
 */
public interface SolicitudesService {

    //Consulta los tipos de pruebas disponibles.
    List<TipoPrueba> consultarTiposPrueba();

    //Crea una solicitud de prueba diagnostica. Podrá hacerlo un médico autorizado
    SolicitudPrueba crearSolicitud(
            Usuario solicitante,
            SolicitudPrueba datos)
            throws SinPermisoException;

    //Busca una solicitud por su identificador
    Optional<SolicitudPrueba> consultarSolicitud(long id);
}
