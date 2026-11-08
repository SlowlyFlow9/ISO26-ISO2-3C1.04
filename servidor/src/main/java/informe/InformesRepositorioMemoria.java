package informe;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InformesRepositorioMemoria implements InformesRepositorio {
    private final List<Informe> informes = new ArrayList<>();

    @Override
    public void guardar(Informe informe) {
        informes.add(informe);
    }

    @Override
    public List<Informe> obtenerPorPaciente(String idPaciente) {
        return informes.stream()
                .filter(i -> i.getIdPaciente().equals(idPaciente))
                .collect(Collectors.toList());
    }

    public List<Informe> getInformes() {
        return informes;
    }
}