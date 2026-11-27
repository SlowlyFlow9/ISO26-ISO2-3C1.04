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

    public ImagenPrueba conId(long nuevoId) {
        return new ImagenPrueba(
                nuevoId,
                solicitudId,
                nombre,
                tipoContenido,
                contenido,
                medicoId,
                fechaCreacion);
    }

    public ResumenImagenPrueba resumen() {
        return new ResumenImagenPrueba(
                id,
                solicitudId,
                nombre,
                tipoContenido,
                medicoId,
                fechaCreacion);
    }
}