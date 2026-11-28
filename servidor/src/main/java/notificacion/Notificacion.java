package notificacion;

import java.time.LocalDateTime;

public record Notificacion(
        long id,
        long pacienteId,
        String titulo,
        String mensaje,
        LocalDateTime fechaHora,
        boolean leida) {
}
