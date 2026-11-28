package solicitud;

/**
 * CU28 - Regla de prioridad activa de CU37 que el personal administrativo puede aplicar
 * (GET /prioridades/reglas). No incluye los datos de auditoria de la regla.
 */
public record ReglaPrioridadAplicable(
        long id,
        String nombre,
        String descripcion,
        int orden) {

    public static ReglaPrioridadAplicable de(ReglaPrioridad regla) {
        return new ReglaPrioridadAplicable(regla.getId(), regla.getNombre(),
                regla.getDescripcion(), regla.getOrden());
    }
}
