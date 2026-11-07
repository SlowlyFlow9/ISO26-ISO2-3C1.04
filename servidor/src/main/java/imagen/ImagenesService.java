package imagen;

import java.util.List;
import informe.Usuario;

public class ImagenesService {
    private final ImagenesRepositorio repositorio;

    public ImagenesService(ImagenesRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public List<ImagenPrueba> consultarImagenes(Usuario solicitante, String idPrueba) {
        // Validación de permisos estricta
        if (!"Médico".equals(solicitante.getRol())) {
            throw new RuntimeException("Solo los médicos pueden consultar imágenes");
        }
        
        if (idPrueba == null || idPrueba.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID de la prueba no puede estar vacío");
        }

        return repositorio.obtenerPorPrueba(idPrueba);
    }

    public void crearImagen(Usuario solicitante, ImagenPrueba imagen) {
        // Validación de permisos estricta
        if (!"Médico".equals(solicitante.getRol())) {
            throw new RuntimeException("Solo los médicos pueden crear imágenes");
        }
        
        if (imagen.getIdPrueba() == null || imagen.getIdPrueba().trim().isEmpty()) {
            throw new IllegalArgumentException("La imagen debe estar asociada a una prueba");
        }

        if (imagen.getRuta() == null || imagen.getRuta().trim().isEmpty()) {
            throw new IllegalArgumentException("La ruta de la imagen no puede estar vacía");
        }

        repositorio.guardar(imagen);
    }
}