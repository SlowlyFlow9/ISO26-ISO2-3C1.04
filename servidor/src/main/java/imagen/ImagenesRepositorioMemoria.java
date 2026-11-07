package imagen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ImagenesRepositorioMemoria implements ImagenesRepositorio {
    private final List<ImagenPrueba> imagenes = new ArrayList<>();

    @Override
    public void guardar(ImagenPrueba imagen) {
        imagenes.add(imagen);
    }

    @Override
    public List<ImagenPrueba> obtenerPorPrueba(String idPrueba) {
        return imagenes.stream()
                .filter(img -> img.getIdPrueba().equals(idPrueba))
                .collect(Collectors.toList());
    }
}