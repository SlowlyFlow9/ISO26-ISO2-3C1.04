package imagen;

import java.util.List;
import java.util.Optional;

public interface ImagenesRepositorio {

    ImagenPrueba guardar(ImagenPrueba imagen);

    Optional<ImagenPrueba> buscarPorId(long id);

    List<ImagenPrueba> listarPorSolicitud(long solicitudId);
}