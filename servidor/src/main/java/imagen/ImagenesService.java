package imagen;

import java.util.List;
import informe.Usuario; // Reutilizamos tu clase Usuario del caso de uso anterior

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
}