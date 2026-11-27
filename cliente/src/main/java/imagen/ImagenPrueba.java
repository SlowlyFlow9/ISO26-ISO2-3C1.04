package imagen;

import java.time.LocalDateTime;

public record ImagenPrueba(
        long id,
        long solicitudId,
        String nombre,
        String tipoContenido,
        String contenido,
        long medicoId,
        LocalDateTime fechaCreacion) {
}