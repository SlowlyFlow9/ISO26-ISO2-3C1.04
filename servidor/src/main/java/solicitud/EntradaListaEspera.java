package solicitud;

import java.time.LocalDateTime;

public record EntradaListaEspera(
        long solicitudId,
        long pacienteId,
        long tipoPruebaId,
        Prioridad prioridad,
        LocalDateTime fechaSolicitud) {
}
