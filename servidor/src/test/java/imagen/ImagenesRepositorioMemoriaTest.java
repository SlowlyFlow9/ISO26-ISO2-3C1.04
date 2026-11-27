package imagen;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImagenesRepositorioMemoriaTest {

    private ImagenesRepositorioMemoria repositorio;

    @BeforeEach
    void preparar() {
        repositorio = new ImagenesRepositorioMemoria();
    }

    private ImagenPrueba imagen(long solicitudId) {

        return new ImagenPrueba(
                0,
                solicitudId,
                "radiografia.png",
                "image/png",
                "aW1hZ2Vu",
                1L,
                LocalDateTime.of(2026, 11, 27, 10, 0));
    }

    @Test
    @DisplayName("Las imágenes reciben identificadores distintos")
    void asignarIdentificadores() {

        ImagenPrueba primera = repositorio.guardar(imagen(1));
        ImagenPrueba segunda = repositorio.guardar(imagen(1));

        assertAll(
                () -> assertEquals(1L, primera.id()),
                () -> assertEquals(2L, segunda.id())
        );
    }

    @Test
    @DisplayName("Buscar una imagen devuelve sus datos")
    void buscarImagen() {

        ImagenPrueba guardada = repositorio.guardar(imagen(1));

        assertEquals(
                guardada,
                repositorio.buscarPorId(guardada.id())
                        .orElseThrow());
    }

    @Test
    @DisplayName("Buscar una imagen inexistente devuelve vacío")
    void buscarInexistente() {

        assertTrue(repositorio.buscarPorId(999).isEmpty());
    }

    @Test
    @DisplayName("El listado contiene solo imágenes de la solicitud")
    void listarPorSolicitud() {

        ImagenPrueba primera = repositorio.guardar(imagen(1));
        ImagenPrueba segunda = repositorio.guardar(imagen(1));

        repositorio.guardar(imagen(2));

        assertEquals(
                java.util.List.of(primera, segunda),
                repositorio.listarPorSolicitud(1));
    }

    @Test
    @DisplayName("Una solicitud sin imágenes devuelve una lista vacía")
    void listadoVacio() {

        assertTrue(repositorio.listarPorSolicitud(1).isEmpty());
    }

    @Test
    @DisplayName("No se puede guardar una imagen nula")
    void imagenNula() {

        assertThrows(
                IllegalArgumentException.class,
                () -> repositorio.guardar(null));
    }

    @Test
    @DisplayName("Una imagen nueva no puede tener ID asignado")
    void imagenConId() {

        ImagenPrueba datos = imagen(1).conId(10);

        assertThrows(
                IllegalArgumentException.class,
                () -> repositorio.guardar(datos));

        assertTrue(repositorio.listarPorSolicitud(1).isEmpty());
    }
}