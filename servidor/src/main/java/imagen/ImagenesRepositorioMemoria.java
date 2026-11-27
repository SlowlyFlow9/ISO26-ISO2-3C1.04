package imagen;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ImagenesRepositorioMemoria
        implements ImagenesRepositorio {

    private final Map<Long, ImagenPrueba> imagenes =
            new LinkedHashMap<>();

    private long siguienteId = 1;

    @Override
    public synchronized ImagenPrueba guardar(
            ImagenPrueba imagen) {

        if (imagen == null) {
            throw new IllegalArgumentException(
                    "La imagen es obligatoria");
        }

        if (imagen.id() != 0) {
            throw new IllegalArgumentException(
                    "Una imagen nueva no puede tener ID asignado");
        }

        ImagenPrueba guardada =
                imagen.conId(siguienteId++);

        imagenes.put(guardada.id(), guardada);

        return guardada;
    }

    @Override
    public synchronized Optional<ImagenPrueba> buscarPorId(
            long id) {

        return Optional.ofNullable(imagenes.get(id));
    }

    @Override
    public synchronized List<ImagenPrueba> listarPorSolicitud(
            long solicitudId) {

        return imagenes.values().stream()
                .filter(imagen ->
                        imagen.solicitudId() == solicitudId)
                .toList();
    }
}