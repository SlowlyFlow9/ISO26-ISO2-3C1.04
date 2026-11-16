package agenda;

import java.util.ArrayList;
import java.util.List;

public final class ValidadorAgendaMedica {

    private ValidadorAgendaMedica() {
    }

    public static List<String> validar(
            AgendaMedica agenda) {

        List<String> errores = new ArrayList<>();

        if (agenda == null) {
            errores.add(
                    "Los datos de la agenda son obligatorios");
            return errores;
        }

        if (agenda.getMedicoId() <= 0) {
            errores.add(
                    "El médico es obligatorio");
        }

        if (agenda.getFecha() == null) {
            errores.add(
                    "La fecha es obligatoria");
        }

        if (agenda.getHoraInicio() == null) {
            errores.add(
                    "La hora de inicio es obligatoria");
        }

        if (agenda.getHoraFin() == null) {
            errores.add(
                    "La hora de fin es obligatoria");
        }

        if (agenda.getHoraInicio() != null
                && agenda.getHoraFin() != null
                && !agenda.getHoraFin()
                        .isAfter(agenda.getHoraInicio())) {

            errores.add(
                    "La hora de fin debe ser posterior "
                            + "a la hora de inicio");
        }

        return errores;
    }
}