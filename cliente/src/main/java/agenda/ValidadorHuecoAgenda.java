package agenda;

import java.util.ArrayList;
import java.util.List;

/**
 * CU29 - Validación de los datos del formulario.
 *
 * La validación definitiva de negocio
 * corresponde al servidor.
 */

public final class ValidadorHuecoAgenda {

    private ValidadorHuecoAgenda() {
    }

    public static List<String> validar(HuecoAgenda datos) {

        List<String> errores = new ArrayList<>();

        if (datos == null) {
            errores.add("Los datos del hueco son obligatorios");
            return errores;
        }

        if (datos.getAgendaId() <= 0) {
            errores.add("Seleccione una agenda válida");
        }

        if (datos.getHoraInicio() == null) {
            errores.add("La hora de inicio es obligatoria");
        }

        if (datos.getHoraFin() == null) {
            errores.add("La hora de fin es obligatoria");
        }

        if (datos.getHoraInicio() != null
                && datos.getHoraFin() != null
                && !datos.getHoraFin()
                        .isAfter(datos.getHoraInicio())) {

            errores.add(
                    "La hora de fin debe ser posterior a la de inicio");
        }

        return errores;
    }


}
