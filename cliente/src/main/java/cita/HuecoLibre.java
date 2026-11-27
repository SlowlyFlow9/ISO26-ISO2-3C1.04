package cita;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * CU03: hueco libre que el paciente puede elegir para cambiar su cita.
 * Es el mismo contenido que devuelve el servidor en GET /citas/huecos-libres.
 */
public record HuecoLibre(
        long id,
        long medicoId,
        LocalDateTime fechaHora) {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public String toString() {
        return fechaHora.format(FORMATO)
                + " (médico " + medicoId + ")";
    }
}
