package solicitud;

import java.util.List;

import paciente.Usuario;
import tipoprueba.TipoPrueba;

/**
 * Operaciones que puede utilizar el cliente de CU11, CU12 y CU13.
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

    //CU13: consultar las solicitudes que cumplen el filtro
    List<SolicitudPrueba> consultarSolicitudes(
            Usuario medico,
            FiltroSolicitudes filtro);

    //CU13: modificar los datos clinicos de una solicitud pendiente de citacion
    SolicitudPrueba modificarSolicitud(
            Usuario medico,
            long id,
            DatosModificacionSolicitud datos);

    //CU13: anular una solicitud pendiente de citacion indicando el motivo
    SolicitudPrueba anularSolicitud(
            Usuario medico,
            long id,
            String motivo);
}
