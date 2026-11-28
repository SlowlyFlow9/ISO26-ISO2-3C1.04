package solicitud;

import java.util.ArrayList;
import java.util.List;

/**
 * CU28 - Cliente. Valida el formato de un cambio de prioridad antes de enviarlo.
 * Solo comprueba el formato (D-013); las reglas de negocio las aplica el servidor (D-038).
 */
public final class ValidadorCambioPrioridad {

    private ValidadorCambioPrioridad() { }

    /** Devuelve la lista de errores; vacia si el cambio tiene un formato valido. */
    public static List<String> validar(Prioridad nueva, Prioridad actual,
            ReglaPrioridadAplicable regla, String motivo) {

        List<String> errores = new ArrayList<>();
        if (nueva == null) {
            errores.add("Selecciona la nueva prioridad");
        } else if (nueva == actual) {
            errores.add("La nueva prioridad debe ser distinta de la actual");
        }
        if (regla == null) errores.add("Selecciona la regla de prioridad aplicada");
        if (motivo == null || motivo.isBlank()) errores.add("El motivo del cambio es obligatorio");
        return errores;
    }
}
