package cita;

import java.time.LocalDateTime;

/**
 * CU03: hueco libre que el paciente puede elegir para cambiar su cita.
 * Solo expone lo necesario: el identificador, el médico y la fecha y hora.
 */
public record HuecoLibre(
        long id,
        long medicoId,
        LocalDateTime fechaHora) {
}
