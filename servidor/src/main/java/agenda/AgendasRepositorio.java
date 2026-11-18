package agenda;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AgendasRepositorio {

    AgendaMedica guardar(AgendaMedica agenda);

    Optional<AgendaMedica> buscarPorId(long id);

    List<AgendaMedica> listar(
            Long medicoId,
            LocalDate fecha,
            Boolean activo);
}