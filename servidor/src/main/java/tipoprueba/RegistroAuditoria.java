package tipoprueba;

import java.util.List;

/** Puerto de auditoría. La consulta completa llegará con CU40 y CU41 (It. 12). */
public interface RegistroAuditoria {
    void registrar(EventoAuditoria evento);

    List<EventoAuditoria> eventos();
}
