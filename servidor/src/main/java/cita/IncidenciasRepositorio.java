package cita;

import java.util.List;

public interface IncidenciasRepositorio {

    IncidenciaCita guardar(IncidenciaCita incidencia);

    List<IncidenciaCita> listarPorCita(long citaId);
}
