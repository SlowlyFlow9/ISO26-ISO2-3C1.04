package solicitud;

import java.util.List;
import java.util.Optional;

public interface ReglasPrioridadRepositorio {

    ReglaPrioridad guardar(ReglaPrioridad regla);

    Optional<ReglaPrioridad> buscarPorId(long id);

    Optional<ReglaPrioridad> buscarPorNombre(String nombre);

    Optional<ReglaPrioridad> buscarPorOrden(int orden);

    List<ReglaPrioridad> todas();
}
