package solicitud;

import java.util.List;

/**
 * CU28 - Almacenamiento del historial de cambios de prioridad (D-038).
 */
public interface CambiosPrioridadRepositorio {

    //Guarda un cambio; si su id es 0 se le asigna uno nuevo
    CambioPrioridad guardar(CambioPrioridad cambio);

    //Cambios de una solicitud, del mas reciente al mas antiguo
    List<CambioPrioridad> listarPorSolicitud(long solicitudId);
}
