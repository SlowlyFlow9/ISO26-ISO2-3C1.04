package cita;

import java.util.List;
import java.util.Optional;

public interface CitasRepositorio {

    Cita guardar(Cita cita);

    Optional<Cita> buscarPorId(long id);

    List<Cita> listarPorPaciente(long pacienteId);

    // CU25: la cita PROGRAMADA o CONFIRMADA que ocupa un hueco, si la hay
    Optional<Cita> buscarActivaPorHueco(long huecoId);
}