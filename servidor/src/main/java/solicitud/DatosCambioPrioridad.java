package solicitud;

/**
 * CU28 - Datos de un cambio de prioridad (D-038). Es el cuerpo de PUT /prioridades/{id} (D-039).
 * La regla es un Long para poder detectar que no se ha enviado.
 */
public record DatosCambioPrioridad(
        Prioridad prioridad,
        Long reglaPrioridadId,
        String motivo) {
}
