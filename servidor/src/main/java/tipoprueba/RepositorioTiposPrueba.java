package tipoprueba;

import java.util.List;
import java.util.Optional;

/** Persistencia de tipos de prueba. El servicio solo depende de esta interfaz. */
public interface RepositorioTiposPrueba {

    /** Inserta (si id == 0, asignando uno nuevo) o actualiza. Devuelve la entidad guardada. */
    TipoPrueba guardar(TipoPrueba tipo);

    Optional<TipoPrueba> buscarPorId(long id);

    /** Búsqueda exacta por nombre, sin distinguir mayúsculas ni espacios extremos. */
    Optional<TipoPrueba> buscarPorNombre(String nombre);

    List<TipoPrueba> todos();
}
