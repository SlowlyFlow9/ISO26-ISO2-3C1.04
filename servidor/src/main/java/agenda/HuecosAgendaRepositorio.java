package agenda;

import java.util.List;
import java.util.Optional;

/**
 * CU29 - Gestionar huecos de agenda.
 *
 * Define las operaciones de almacenamiento
 * y consulta de los huecos de una agenda médica.
 */
public interface HuecosAgendaRepositorio {

    /**
     * Guarda un hueco nuevo o actualizado.
     *
     * @param hueco Hueco que se quiere almacenar.
     * @return Hueco almacenado.
     */
    HuecoAgenda guardar(HuecoAgenda hueco);

    /**
     * Busca un hueco por su identificador.
     *
     * @param id Identificador del hueco.
     * @return Hueco encontrado o Optional vacío.
     */
    Optional<HuecoAgenda> buscarPorId(long id);

    /**
     * Obtiene los huecos asociados a una agenda.
     *
     * @param agendaId Identificador de la agenda.
     * @return Lista de huecos de esa agenda.
     */
    List<HuecoAgenda> listarPorAgenda(long agendaId);
}
