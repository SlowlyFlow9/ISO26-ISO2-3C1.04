package protocolo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProtocolosPruebaRepositorioMemoria
        implements ProtocolosPruebaRepositorio {

    private final Map<Long, ProtocoloPrueba> protocolos =
            new LinkedHashMap<>();

    private long siguienteId = 1;

    @Override
    public synchronized ProtocoloPrueba guardar(
            ProtocoloPrueba protocolo) {

        if (protocolo.getId() == null) {
            protocolo.setId(siguienteId++);
        }

        protocolos.put(protocolo.getId(), protocolo);

        return protocolo;
    }

    @Override
    public synchronized Optional<ProtocoloPrueba> buscarPorId(long id) {
        return Optional.ofNullable(protocolos.get(id));
    }

    @Override
    public synchronized List<ProtocoloPrueba> listar(
            Long tipoPruebaId,
            Boolean activo) {

        return protocolos.values().stream()
                .filter(p -> tipoPruebaId == null
                        || tipoPruebaId.equals(p.getTipoPruebaId()))
                .filter(p -> activo == null
                        || activo.equals(p.isActivo()))
                .toList();
    }
}