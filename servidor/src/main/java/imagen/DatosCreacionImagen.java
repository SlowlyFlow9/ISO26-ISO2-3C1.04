package imagen;

public record DatosCreacionImagen(
        long solicitudId,
        String nombre,
        String tipoContenido,
        String contenido) {
}