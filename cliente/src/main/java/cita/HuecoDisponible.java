package cita;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * CU25: un hueco libre de una agenda, listo para dar una cita.
 * El hueco de CU29 no lleva fecha ni médico; aquí se muestran juntos.
 */
public record HuecoDisponible(
        long id,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin,
        long medicoId) {

    private static final DateTimeFormatter DIA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String toString() {
        return fecha.format(DIA) + " "
                + horaInicio + "-" + horaFin
                + " (médico " + medicoId + ")";
    }
}
