package solicitud;

/**
 * CU28 - Cliente. Regla de prioridad activa (configurada en CU37) que el personal
 * administrativo puede aplicar al cambiar una prioridad (D-038).
 */
public record ReglaPrioridadAplicable(
        long id,
        String nombre,
        String descripcion,
        int orden) {

    //Texto que se muestra en el desplegable de reglas
    @Override
    public String toString() {
        return nombre;
    }
}
