package solicitud;

import java.util.List;

import paciente.SinPermisoException;
import paciente.Usuario;

/**
 * CU28 - Gestion de prioridades por el personal administrativo (D-038).
 * Solo el rol Personal administrativo puede usar estas operaciones (D-016).
 */
public interface PrioridadesService {

    //Solicitudes en PENDIENTE_CITACION ordenadas por prioridad, fecha e id; prioridad nula = todas
    List<SolicitudPrioridad> consultarPendientes(
            Usuario solicitante,
            Prioridad prioridad)
            throws SinPermisoException;

    //Reglas de prioridad activas de CU37, ordenadas por su orden
    List<ReglaPrioridadAplicable> consultarReglasActivas(
            Usuario solicitante)
            throws SinPermisoException;

    //Cambia la prioridad de una solicitud pendiente y guarda el cambio en el historial
    SolicitudPrioridad cambiarPrioridad(
            Usuario solicitante,
            long solicitudId,
            DatosCambioPrioridad datos)
            throws SinPermisoException;

    //Cambios de prioridad de una solicitud, del mas reciente al mas antiguo
    List<CambioPrioridad> consultarHistorial(
            Usuario solicitante,
            long solicitudId)
            throws SinPermisoException;
}
