package imagen;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ValidadorImagenTest {

    private static String png;
    private static String jpeg;

    @BeforeAll
    static void preparar() throws Exception {

        png = crearImagen("png");
        jpeg = crearImagen("jpeg");
    }

    static String crearImagen(String formato)
            throws Exception {

        BufferedImage imagen = new BufferedImage(
                20, 10, BufferedImage.TYPE_INT_RGB);

        imagen.setRGB(0, 0, Color.RED.getRGB());

        ByteArrayOutputStream salida =
                new ByteArrayOutputStream();

        if (!ImageIO.write(imagen, formato, salida)) {

            throw new IllegalArgumentException(
                    "Formato de prueba no disponible");
        }

        return Base64.getEncoder()
                .encodeToString(salida.toByteArray());
    }

    private DatosCreacionImagen datosValidos() {

        return new DatosCreacionImagen(
                1,
                "radiografia.png",
                "image/png",
                png);
    }

    @Test
    @DisplayName("Una imagen PNG real es válida")
    void pngValido() {

        assertDoesNotThrow(
                () -> ValidadorImagen.validar(datosValidos()));
    }

    @Test
    @DisplayName("Una imagen JPEG real es válida")
    void jpegValido() {

        var datos = new DatosCreacionImagen(
                1, "imagen.jpg", "image/jpeg", jpeg);

        assertDoesNotThrow(
                () -> ValidadorImagen.validar(datos));
    }

    @Test
    @DisplayName("Los datos nulos son rechazados")
    void datosNulos() {

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> ValidadorImagen.validar(null));
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void solicitudInvalida(long id) {

        var datos = new DatosCreacionImagen(
                id, "imagen.png", "image/png", png);

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> ValidadorImagen.validar(datos));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ",
            "../imagen.png",
            "carpeta/imagen.png",
            "carpeta\\imagen.png",
            ".",
            "..",
            "imagen\n.png"
    })
    void nombreInvalido(String nombre) {

        var datos = new DatosCreacionImagen(
                1, nombre, "image/png", png);

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> ValidadorImagen.validar(datos));
    }

    @Test
    @DisplayName("Se admite un nombre de 255 caracteres")
    void nombreLimiteValido() {

        String nombre = "a".repeat(251) + ".png";

        var datos = new DatosCreacionImagen(
                1, nombre, "image/png", png);

        assertDoesNotThrow(
                () -> ValidadorImagen.validar(datos));
    }

    @Test
    @DisplayName("Se rechaza un nombre de 256 caracteres")
    void nombreDemasiadoLargo() {

        String nombre = "a".repeat(252) + ".png";

        var datos = new DatosCreacionImagen(
                1, nombre, "image/png", png);

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> ValidadorImagen.validar(datos));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "image/gif",
            "application/pdf",
            "application/dicom",
            "text/plain"
    })
    void formatoNoPermitido(String tipo) {

        var datos = new DatosCreacionImagen(
                1, "imagen.png", tipo, png);

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> ValidadorImagen.validar(datos));
    }

    @Test
    @DisplayName("No se admite un PNG declarado como JPEG")
    void formatoIncorrecto() {

        var datos = new DatosCreacionImagen(
                1, "imagen.jpg", "image/jpeg", png);

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> ValidadorImagen.validar(datos));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "%%%"})
    void contenidoInvalido(String contenido) {

        var datos = new DatosCreacionImagen(
                1, "imagen.png", "image/png", contenido);

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> ValidadorImagen.validar(datos));
    }

    @Test
    @DisplayName("Un archivo de texto no es una imagen")
    void contenidoNoEsImagen() {

        String contenido = Base64.getEncoder()
                .encodeToString("Texto".getBytes());

        var datos = new DatosCreacionImagen(
                1, "imagen.png", "image/png", contenido);

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> ValidadorImagen.validar(datos));
    }

    @Test
    @DisplayName("Se rechaza un contenido de 5 MiB más un byte")
    void imagenDemasiadoGrande() {

        byte[] bytes =
                new byte[ValidadorImagen.MAX_BYTES + 1];

        String contenido = Base64.getEncoder()
                .encodeToString(bytes);

        var datos = new DatosCreacionImagen(
                1, "imagen.png", "image/png", contenido);

        var error = assertThrows(
                DatosImagenInvalidosException.class,
                () -> ValidadorImagen.validar(datos));

        assertTrue(error.getMessage().contains("tamaño"));
    }

    @Test
    @DisplayName("Se rechaza el Base64 que supera el límite")
    void base64DemasiadoGrande() {

        String contenido = "A".repeat(
                4 * ((ValidadorImagen.MAX_BYTES + 2) / 3) + 1);

        var datos = new DatosCreacionImagen(
                1, "imagen.png", "image/png", contenido);

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> ValidadorImagen.validar(datos));
    }

    @Test
    @DisplayName("Se admite una imagen de 16 millones de píxeles")
    void dimensionesLimiteValidas() throws Exception {

        String contenido = crearImagenDimensiones(4000, 4000);

        var datos = new DatosCreacionImagen(
                1, "imagen.png", "image/png", contenido);

        assertDoesNotThrow(
                () -> ValidadorImagen.validar(datos));
    }

    @Test
    @DisplayName("Se rechazan dimensiones superiores al límite")
    void dimensionesExcesivas() throws Exception {

        String contenido = crearImagenDimensiones(4001, 4000);

        var datos = new DatosCreacionImagen(
                1, "imagen.png", "image/png", contenido);

        var error = assertThrows(
                DatosImagenInvalidosException.class,
                () -> ValidadorImagen.validar(datos));

        assertTrue(
                error.getMessage().contains("dimensiones"));
    }

    private String crearImagenDimensiones(
            int ancho, int alto) throws Exception {

        BufferedImage imagen = new BufferedImage(
                ancho,
                alto,
                BufferedImage.TYPE_BYTE_BINARY);

        ByteArrayOutputStream salida =
                new ByteArrayOutputStream();

        ImageIO.write(imagen, "png", salida);

        return Base64.getEncoder()
                .encodeToString(salida.toByteArray());
    }
}