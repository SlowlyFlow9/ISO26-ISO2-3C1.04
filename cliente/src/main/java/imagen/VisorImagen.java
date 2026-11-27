package imagen;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;

public final class VisorImagen {

    private VisorImagen() {
    }

    public static BufferedImage decodificar(String contenido) {

        if (contenido == null || contenido.isBlank()) {

            throw new IllegalArgumentException(
                    "La imagen no tiene contenido");
        }

        try {

            byte[] datos = Base64.getDecoder()
                    .decode(contenido);

            BufferedImage imagen = ImageIO.read(
                    new ByteArrayInputStream(datos));

            if (imagen == null) {

                throw new IllegalArgumentException(
                        "El contenido no corresponde a una imagen");
            }

            return imagen;

        } catch (IllegalArgumentException | IOException e) {

            throw new IllegalArgumentException(
                    "No se pudo visualizar la imagen", e);
        }
    }
}