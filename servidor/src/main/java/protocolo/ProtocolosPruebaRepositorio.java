package protocolo;

import java.util.List;
import java.util.Optional;

public interface ProtocolosPruebaRepositorio {

    ProtocoloPrueba guardar(ProtocoloPrueba protocolo);

    Optional<ProtocoloPrueba> buscarPorId(long id);

    List<ProtocoloPrueba> listar(Long tipoPruebaId, Boolean activo);
}