package solicitud;

import java.util.List;

import paciente.Usuario;
import tipoprueba.TipoPrueba;

/**
 * Operaciones que puede utilizar el cliente de CU11.
 */
public interface SolicitudesService {

    //Consultar los tipos de prueba disponibles
    List<TipoPrueba> consultarTiposPrueba(Usuario medico);

    //Registrar una nueva prescripcion
    SolicitudPrueba crearSolicitud(
            Usuario medico,
            SolicitudPrueba datos);

    //Consultar una solicitud por su identificador
    SolicitudPrueba consultarSolicitud(
            Usuario medico,
            long id);
}
