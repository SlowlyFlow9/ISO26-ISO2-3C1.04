package imagen;

import java.util.List;

public interface ImagenesRepositorio {
    List<ImagenPrueba> obtenerPorPrueba(String idPrueba);
    void guardar(ImagenPrueba imagen); // Lo dejamos preparado ya para el CU18
}