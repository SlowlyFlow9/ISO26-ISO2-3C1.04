package solicitud;

import java.time.LocalDateTime;

/**
 * CU28 - Cliente. Cambio de prioridad registrado en el historial de una solicitud (D-038):
 * prioridad anterior y nueva, regla aplicada, motivo, quien lo hizo y cuando (D-017).
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
}
