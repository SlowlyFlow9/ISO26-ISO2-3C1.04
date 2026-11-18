package tipoprueba;

import java.util.ArrayList;
import java.util.List;

/** Auditoría en memoria. */
public class RegistroAuditoriaMemoria implements RegistroAuditoria {
    private final List<EventoAuditoria> eventos = new ArrayList<>();

    @Override
    public synchronized void registrar(EventoAuditoria evento) {
        eventos.add(evento);
    }

    @Override
    public synchronized List<EventoAuditoria> eventos() {
        return new ArrayList<>(eventos);
    }
}
