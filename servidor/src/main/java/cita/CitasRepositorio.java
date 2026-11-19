package cita;

import java.util.List;
import java.util.Optional;

public interface CitasRepositorio {

    Cita guardar(Cita cita);

    Optional<Cita> buscarPorId(long id);

    List<Cita> listarPorPaciente(long pacienteId);
}