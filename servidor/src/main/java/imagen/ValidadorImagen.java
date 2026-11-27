package imagen;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;

public final class ValidadorImagen {

    public static final int MAX_BYTES =
            5 * 1024 * 1024;

    public static final long MAX_PIXELES =
            16_000_000L;

    private static final int MAX_BASE64 =
            4 * ((MAX_BYTES + 2) / 3);

    private ValidadorImagen() {
    }

    public static void validar(DatosCreacionImagen datos)
            throws DatosImagenInvalidosException {

        if (datos == null) {
            throw new DatosImagenInvalidosException(
                    "Los datos de la imagen son obligatorios");
        }

        if (datos.solicitudId() <= 0) {
            throw new DatosImagenInvalidosException(
                    "El identificador de solicitud debe ser positivo");
        }

        String nombre = datos.nombre();

        if (nombre == null
                || nombre.isBlank()
                || nombre.length() > 255
                || nombre.contains("/")
                || nombre.contains("\\")
                || nombre.equals(".")
                || nombre.equals("..")
                || nombre.chars().anyMatch(Character::isISOControl)) {

            throw new DatosImagenInvalidosException(
                    "El nombre del archivo no es válido");
        }

        String tipo = datos.tipoContenido();

        if (!"image/png".equals(tipo)
                && !"image/jpeg".equals(tipo)) {

            throw new DatosImagenInvalidosException(
                    "Solo se permiten imágenes PNG y JPEG");
        }

        String contenido = datos.contenido();

        if (contenido == null || contenido.isBlank()) {

            throw new DatosImagenInvalidosException(
                    "El contenido de la imagen es obligatorio");
        }

        if (contenido.length() > MAX_BASE64) {

            throw new DatosImagenInvalidosException(
                    "La imagen supera el tamaño máximo de 5 MiB");
        }

        byte[] bytes;

        try {

            bytes = Base64.getDecoder().decode(contenido);

        } catch (IllegalArgumentException e) {

            throw new DatosImagenInvalidosException(
                    "El contenido Base64 no es válido");
        }

        if (bytes.length == 0 || bytes.length > MAX_BYTES) {

            throw new DatosImagenInvalidosException(
                    "El tamaño de la imagen no es válido");
        }

        validarContenido(bytes, tipo);
    }

    private static void validarContenido(
            byte[] bytes,
            String tipo)
            throws DatosImagenInvalidosException {

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

                boolean formatoCorrecto =
                        ("image/png".equals(tipo)
                                && formato.equalsIgnoreCase("png"))
                        || ("image/jpeg".equals(tipo)
                                && formato.equalsIgnoreCase("jpeg"));

                if (!formatoCorrecto) {

                    throw new DatosImagenInvalidosException(
                            "El formato declarado no coincide "
                            + "con el contenido de la imagen");
                }

                int ancho = lector.getWidth(0);
                int alto = lector.getHeight(0);

                if (ancho <= 0
                        || alto <= 0
                        || (long) ancho * alto > MAX_PIXELES) {

                    throw new DatosImagenInvalidosException(
                            "Las dimensiones de la imagen "
                            + "superan el límite permitido");
                }

                if (lector.read(0) == null) {

                    throw new DatosImagenInvalidosException(
                            "No se pudo leer la imagen");
                }

            } finally {

                lector.dispose();
            }

        } catch (IOException | RuntimeException e) {

            throw new DatosImagenInvalidosException(
                    "La imagen está dañada o no se puede leer");
        }
    }
}