package imagen;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class VisorImagenTest {

    static String crearImagenPrueba() throws Exception {

        BufferedImage imagen =
                new BufferedImage(
                        20,
                        10,
                        BufferedImage.TYPE_INT_RGB);

        imagen.setRGB(0, 0, Color.RED.getRGB());

        ByteArrayOutputStream salida =
                new ByteArrayOutputStream();

        ImageIO.write(imagen, "png", salida);

        return Base64.getEncoder()
                .encodeToString(salida.toByteArray());
    }

    @Test
    @DisplayName("Una imagen PNG válida se decodifica correctamente")
    void decodificarImagen() throws Exception {

        BufferedImage resultado =
                VisorImagen.decodificar(
                        crearImagenPrueba());

        assertAll(
                () -> assertEquals(20, resultado.getWidth()),
                () -> assertEquals(10, resultado.getHeight()),
                () -> assertEquals(
                        Color.RED.getRGB(),
                        resultado.getRGB(0, 0))
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void contenidoVacio(String contenido) {

        assertThrows(
                IllegalArgumentException.class,
                () -> VisorImagen.decodificar(contenido));
    }

    @Test
    @DisplayName("Se rechaza contenido Base64 incorrecto")
    void base64Incorrecto() {

        assertThrows(
                IllegalArgumentException.class,
                () -> VisorImagen.decodificar("%%%"));
    }

    @Test
    @DisplayName("Se rechaza contenido que no representa una imagen")
    void contenidoNoEsImagen() {

        String contenido = Base64.getEncoder()
                .encodeToString("Texto".getBytes());

        assertThrows(
                IllegalArgumentException.class,
                () -> VisorImagen.decodificar(contenido));
    }
}