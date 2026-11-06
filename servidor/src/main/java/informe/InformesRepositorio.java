package informe;

import java.util.List;

public interface InformesRepositorio {
    void guardar(Informe informe);
    List<Informe> obtenerPorPaciente(String idPaciente);
}