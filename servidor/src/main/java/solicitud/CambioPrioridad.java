package solicitud;

import java.time.LocalDateTime;

/**
 * CU28 - Cambio de prioridad de una solicitud (D-038): prioridad anterior y nueva,
 * regla de prioridad aplicada (CU37), motivo, quien lo hizo y cuando (D-017).
 */
public record CambioPrioridad(
        long id,
        long solicitudId,
        Prioridad prioridadAnterior,
        Prioridad prioridadNueva,
        long reglaPrioridadId,
        String motivo,
        String realizadoPor,
        LocalDateTime fechaCambio) {

    //Devuelve una copia del cambio con otro id (lo asigna el repositorio)
    public CambioPrioridad conId(long nuevoId) {
        return new CambioPrioridad(nuevoId, solicitudId, prioridadAnterior, prioridadNueva,
                reglaPrioridadId, motivo, realizadoPor, fechaCambio);
    }
}
