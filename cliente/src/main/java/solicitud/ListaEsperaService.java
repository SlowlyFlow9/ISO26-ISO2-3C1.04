package solicitud;

import java.util.List;

import paciente.Usuario;

public interface ListaEsperaService {

    List<EntradaListaEspera> consultar(
            Usuario solicitante,
            Prioridad prioridad);
}
