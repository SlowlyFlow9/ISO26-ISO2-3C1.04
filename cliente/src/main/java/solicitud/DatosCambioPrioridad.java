package solicitud;

/**
 * CU28 - Cliente. Datos de un cambio de prioridad (D-038): la nueva prioridad,
 * la regla de prioridad aplicada y el motivo. Es el cuerpo de PUT /prioridades/{id} (D-039).
 */
public record DatosCambioPrioridad(
        Prioridad prioridad,
        long reglaPrioridadId,
        String motivo) {
}
