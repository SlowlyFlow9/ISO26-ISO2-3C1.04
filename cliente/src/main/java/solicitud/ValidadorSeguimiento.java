package solicitud;

import java.util.ArrayList;
import java.util.List;

/**
 * Valida el formato de los datos de periodicidad de una prueba de seguimiento.
 * CU12 - Cliente. Solo comprueba el formato (D-013); las reglas de negocio las aplica el servidor.
 */
public final class ValidadorSeguimiento {

    private ValidadorSeguimiento() { }

    /** Devuelve la lista de errores; vacía si los datos tienen un formato válido. La prescripción original es opcional. */
    public static List<String> validar(String intervalo, UnidadPeriodicidad unidad,
            String repeticiones, String solicitudOrigen) {
        List<String> errores = new ArrayList<>();
        if (!esEnteroPositivo(intervalo)) errores.add("El intervalo debe ser un número entero mayor que 0");
        if (unidad == null) errores.add("Selecciona la unidad de la periodicidad");
        if (!esEnteroPositivo(repeticiones)) errores.add("El número de repeticiones debe ser un número entero mayor que 0");
        if (solicitudOrigen != null && !solicitudOrigen.isBlank() && !esEnteroPositivo(solicitudOrigen))
            errores.add("La prescripción original debe ser un identificador positivo");
        return errores;
    }

    private static boolean esEnteroPositivo(String texto) {
        if (texto == null) return false;
        try {
            return Integer.parseInt(texto.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
