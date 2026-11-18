package solicitud;

import java.util.List;
import java.util.Optional;

import paciente.Usuario;
import paciente.SinPermisoException;
import tipoprueba.TipoPrueba;

/**
 * Operaciones del servicio de solicitudes.
 * CU11 - Prescribir pruebas diagnosticas. CU12 - Pruebas de seguimiento. CU13 - Gestionar solicitudes.
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

    //CU13: solicitudes que cumplen el filtro, ordenadas por prioridad y fecha de solicitud
    List<SolicitudPrueba> consultarSolicitudes(
            Usuario solicitante,
            FiltroSolicitudes filtro)
            throws SinPermisoException;

    //CU13: modifica los datos clinicos (y la periodicidad de un seguimiento) de una solicitud pendiente de citacion
    SolicitudPrueba modificarSolicitud(
            Usuario solicitante,
            long id,
            DatosModificacionSolicitud datos)
            throws SinPermisoException;

    //CU13: anula una solicitud pendiente de citacion guardando el motivo y la fecha
    SolicitudPrueba anularSolicitud(
            Usuario solicitante,
            long id,
            String motivo)
            throws SinPermisoException;
}
