package solicitud;

import java.time.LocalDateTime;

/**
 * CU28 - Cliente. Solicitud pendiente de citacion con su prioridad actual,
 * tal como la devuelve GET /prioridades (D-039). No incluye datos clinicos.
 */
public record SolicitudPrioridad(
        long solicitudId,
        long pacienteId,
        long tipoPruebaId,
        Prioridad prioridad,
        LocalDateTime fechaSolicitud) {
}
