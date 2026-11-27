package imagen;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

public final class CargadorImagen {

    public static final int MAX_BYTES =
            5 * 1024 * 1024;

    public static final long MAX_PIXELES =
            16_000_000L;

    private CargadorImagen() {
    }

    public static ArchivoImagen cargar(Path archivo)
            throws DatosImagenInvalidosException {

        if (archivo == null || !Files.isRegularFile(archivo)) {

            throw new DatosImagenInvalidosException(
                    "Selecciona un archivo válido");
        }

        try {

            byte[] bytes;

            try (var entrada = Files.newInputStream(archivo)) {

                bytes = entrada.readNBytes(MAX_BYTES + 1);
            }

            if (bytes.length == 0 || bytes.length > MAX_BYTES) {

                throw new DatosImagenInvalidosException(
                        "La imagen debe ocupar entre 1 byte y 5 MiB");
            }

            try (var entrada = new MemoryCacheImageInputStream(
                    new ByteArrayInputStream(bytes))) {

                var lectores = ImageIO.getImageReaders(entrada);

                if (!lectores.hasNext()) {

                    throw new DatosImagenInvalidosException(
                            "El archivo no contiene una imagen válida");
                }

                ImageReader lector = lectores.next();

                try {

                    lector.setInput(entrada);

                    String formato = lector.getFormatName();

                    String tipo;

                    if (formato.equalsIgnoreCase("png")) {

                        tipo = "image/png";

                    } else if (formato.equalsIgnoreCase("jpeg")) {

                        tipo = "image/jpeg";

                    } else {

                        throw new DatosImagenInvalidosException(
                                "Solo se permiten imágenes PNG y JPEG");
                    }

                    int ancho = lector.getWidth(0);
                    int alto = lector.getHeight(0);

                    if (ancho <= 0 || alto <= 0
                            || (long) ancho * alto > MAX_PIXELES) {

                        throw new DatosImagenInvalidosException(
                                "Las dimensiones superan el límite");
                    }

                    var imagen = lector.read(0);

                    if (imagen == null) {

                        throw new DatosImagenInvalidosException(
                                "No se pudo leer la imagen");
                    }

                    return new ArchivoImagen(
                            archivo.getFileName().toString(),
                            tipo,
                            Base64.getEncoder().encodeToString(bytes),
                            imagen);

                } finally {

                    lector.dispose();
                }
            }

        } catch (IOException | RuntimeException e) {

            throw new DatosImagenInvalidosException(
                    "No se pudo leer el archivo de imagen");
        }
    }
}