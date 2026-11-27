package imagen;

import java.awt.image.BufferedImage;

public record ArchivoImagen(
        String nombre,
        String tipoContenido,
        String contenido,
        BufferedImage vistaPrevia) {

    public DatosCreacionImagen datos(long solicitudId) {

        return new DatosCreacionImagen(
                solicitudId,
                nombre,
                tipoContenido,
                contenido);
    }
}