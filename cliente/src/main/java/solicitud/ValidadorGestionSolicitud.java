package solicitud;

import java.util.ArrayList;
import java.util.List;

/**
 * CU13 - Cliente. Valida el formato de los datos de la gestion de solicitudes.
 * Solo comprueba el formato (D-013); las reglas de negocio las aplica el servidor.
 */
public final class ValidadorGestionSolicitud {

    private ValidadorGestionSolicitud() { }

    /**
     * Valida la modificacion de una solicitud. Si es un seguimiento, tambien valida la periodicidad.
     * Devuelve la lista de errores; vacia si los datos tienen un formato valido.
     */
    public static List<String> validarModificacion(String indicacion, String justificacion,
            Prioridad prioridad, boolean esSeguimiento,
            String intervalo, UnidadPeriodicidad unidad, String repeticiones) {

        List<String> errores = new ArrayList<>();
        if (estaVacio(indicacion)) errores.add("La indicación clínica es obligatoria");
        if (estaVacio(justificacion)) errores.add("La justificación es obligatoria");
        if (prioridad == null) errores.add("Selecciona una prioridad");
        if (esSeguimiento) {
            //La prescripcion original no se modifica, por eso no se valida aqui
            errores.addAll(ValidadorSeguimiento.validar(intervalo, unidad, repeticiones, null));
        }
        return errores;
    }

    /** Valida la anulacion: el motivo es obligatorio (D-036). */
    public static List<String> validarAnulacion(String motivo) {
        List<String> errores = new ArrayList<>();
        if (estaVacio(motivo)) errores.add("El motivo de la anulación es obligatorio");
        return errores;
    }

    /** Valida el filtro de paciente: vacio o un identificador positivo. */
    public static List<String> validarFiltroPaciente(String pacienteId) {
        List<String> errores = new ArrayList<>();
        if (estaVacio(pacienteId)) return errores;
        try {
            if (Long.parseLong(pacienteId.trim()) <= 0) {
                errores.add("El ID del paciente debe ser un número positivo");
            }
        } catch (NumberFormatException e) {
            errores.add("El ID del paciente debe ser un número positivo");
        }
        return errores;
    }

    private static boolean estaVacio(String texto) {
        return texto == null || texto.isBlank();
    }
}
