package imagen;

import java.time.LocalDateTime;

public record ResumenImagenPrueba(
        long id,
        long solicitudId,
        String nombre,
        String tipoContenido,
        long medicoId,
        LocalDateTime fechaCreacion) {
}