package solicitud;

import java.util.List;

import paciente.Usuario;

/**
 * CU28 - Cliente. Operaciones de la gestion de prioridades que consume la interfaz (D-038 y D-039).
 * Solo las puede usar el rol Personal administrativo.
 */
public interface PrioridadesService {

    //Solicitudes en PENDIENTE_CITACION ordenadas por prioridad, fecha e id; prioridad nula = todas
    List<SolicitudPrioridad> consultarPendientes(
            Usuario solicitante,
            Prioridad prioridad);

    //Reglas de prioridad activas que se pueden aplicar en un cambio
    List<ReglaPrioridadAplicable> consultarReglasActivas(
            Usuario solicitante);

    //Cambia la prioridad de una solicitud pendiente y devuelve la solicitud actualizada
    SolicitudPrioridad cambiarPrioridad(
            Usuario solicitante,
            long solicitudId,
            DatosCambioPrioridad datos);

    //Cambios de prioridad de una solicitud, del mas reciente al mas antiguo
    List<CambioPrioridad> consultarHistorial(
            Usuario solicitante,
            long solicitudId);
}
