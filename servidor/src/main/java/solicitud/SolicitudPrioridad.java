package solicitud;

import java.time.LocalDateTime;

/**
 * CU28 - Solicitud pendiente de citacion con su prioridad, tal como la devuelve
 * GET /prioridades (D-039). No incluye la indicacion clinica ni la justificacion.
 */
public record SolicitudPrioridad(
        long solicitudId,
        long pacienteId,
        long tipoPruebaId,
        Prioridad prioridad,
        LocalDateTime fechaSolicitud) {

    public static SolicitudPrioridad de(SolicitudPrueba solicitud) {
        return new SolicitudPrioridad(solicitud.getId(), solicitud.getPacienteId(),
                solicitud.getTipoPruebaId(), solicitud.getPrioridad(), solicitud.getFechaSolicitud());
    }
}
