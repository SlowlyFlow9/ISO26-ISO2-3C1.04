package imagen;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CargadorImagenTest {

    @TempDir
    Path carpeta;

    static Path crearImagen(
            Path carpeta,
            String formato,
            int ancho,
            int alto) throws Exception {

        BufferedImage imagen = new BufferedImage(
                ancho,
                alto,
                BufferedImage.TYPE_INT_RGB);

        imagen.setRGB(0, 0, Color.RED.getRGB());

        Path archivo =
                carpeta.resolve("imagen." + formato);

        if (!ImageIO.write(imagen, formato, archivo.toFile())) {

            throw new IllegalArgumentException(
                    "Formato de prueba no disponible");
        }

        return archivo;
    }

    @Test
    @DisplayName("Una imagen PNG válida se carga correctamente")
    void cargarPng() throws Exception {

        Path archivo =
                crearImagen(carpeta, "png", 20, 10);

        ArchivoImagen resultado =
                CargadorImagen.cargar(archivo);

        assertAll(
                () -> assertEquals(
                        "imagen.png", resultado.nombre()),
                () -> assertEquals(
                        "image/png", resultado.tipoContenido()),
                () -> assertEquals(
                        20, resultado.vistaPrevia().getWidth()),
                () -> assertEquals(
                        10, resultado.vistaPrevia().getHeight()),
                () -> assertEquals(
                        Color.RED.getRGB(),
                        resultado.vistaPrevia().getRGB(0, 0)),
                () -> assertArrayEquals(
                        Files.readAllBytes(archivo),
                        Base64.getDecoder()
                                .decode(resultado.contenido()))
        );
    }

    @Test
    @DisplayName("Una imagen JPEG válida se carga correctamente")
    void cargarJpeg() throws Exception {

        Path archivo =
                crearImagen(carpeta, "jpeg", 20, 10);

        ArchivoImagen resultado =
                CargadorImagen.cargar(archivo);

        assertAll(
                () -> assertEquals(
                        "image/jpeg", resultado.tipoContenido()),
                () -> assertEquals(
                        20, resultado.vistaPrevia().getWidth()),
                () -> assertEquals(
                        10, resultado.vistaPrevia().getHeight())
        );
    }

    @Test
    @DisplayName("El archivo se convierte en datos de creación")
    void datosCreacion() throws Exception {

        Path archivo =
                crearImagen(carpeta, "png", 20, 10);

        ArchivoImagen cargado =
                CargadorImagen.cargar(archivo);

        DatosCreacionImagen datos =
                cargado.datos(15);

        assertAll(
                () -> assertEquals(
                        15L, datos.solicitudId()),
                () -> assertEquals(
                        cargado.nombre(), datos.nombre()),
                () -> assertEquals(
                        cargado.tipoContenido(),
                        datos.tipoContenido()),
                () -> assertEquals(
                        cargado.contenido(), datos.contenido())
        );
    }

    @Test
    @DisplayName("Un archivo nulo es rechazado")
    void archivoNulo() {

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> CargadorImagen.cargar(null));
    }

    @Test
    @DisplayName("Un archivo inexistente es rechazado")
    void archivoInexistente() {

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> CargadorImagen.cargar(
                        carpeta.resolve("inexistente.png")));
    }

    @Test
    @DisplayName("No se puede seleccionar un directorio")
    void directorio() {

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> CargadorImagen.cargar(carpeta));
    }

    @Test
    @DisplayName("Un archivo vacío no es válido")
    void archivoVacio() throws Exception {

        Path archivo =
                Files.createFile(carpeta.resolve("vacio.png"));

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> CargadorImagen.cargar(archivo));
    }

    @Test
    @DisplayName("Un archivo de texto no se admite como imagen")
    void archivoDeTexto() throws Exception {

        Path archivo = Files.writeString(
                carpeta.resolve("falso.png"),
                "Esto no es una imagen");

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> CargadorImagen.cargar(archivo));
    }

    @ParameterizedTest
    @ValueSource(strings = {"gif", "bmp"})
    void formatoNoPermitido(String formato) throws Exception {

        Path archivo =
                crearImagen(carpeta, formato, 20, 10);

        assertThrows(
                DatosImagenInvalidosException.class,
                () -> CargadorImagen.cargar(archivo));
    }

    @Test
    @DisplayName("Un archivo mayor de 5 MiB es rechazado")
    void archivoDemasiadoGrande() throws Exception {

        Path archivo = Files.write(
                carpeta.resolve("grande.png"),
                new byte[CargadorImagen.MAX_BYTES + 1]);

        var error = assertThrows(
                DatosImagenInvalidosException.class,
                () -> CargadorImagen.cargar(archivo));

        assertTrue(error.getMessage().contains("5 MiB"));
    }

    @Test
    @DisplayName("Se comprueba el límite de dimensiones")
    void dimensionesExcesivas() throws Exception {

        BufferedImage imagen = new BufferedImage(
                4001,
                4000,
                BufferedImage.TYPE_BYTE_BINARY);

        Path archivo = carpeta.resolve("grande.png");

        ImageIO.write(imagen, "png", archivo.toFile());

        var error = assertThrows(
                DatosImagenInvalidosException.class,
                () -> CargadorImagen.cargar(archivo));

        assertTrue(
                error.getMessage().contains("dimensiones"));
    }

    @Test
    @DisplayName("Se admite el límite de 16 millones de píxeles")
    void dimensionesLimite() throws Exception {

        BufferedImage imagen = new BufferedImage(
                4000,
                4000,
                BufferedImage.TYPE_BYTE_BINARY);

        Path archivo = carpeta.resolve("limite.png");

        ImageIO.write(imagen, "png", archivo.toFile());

        var resultado = CargadorImagen.cargar(archivo);

        assertEquals(
                16_000_000L,
                (long) resultado.vistaPrevia().getWidth()
                        * resultado.vistaPrevia().getHeight());
    }
}