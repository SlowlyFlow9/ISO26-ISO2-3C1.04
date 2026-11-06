package informe;

import java.util.ArrayList;
import java.util.List;

public class InformesRepositorioMemoria implements InformesRepositorio {
    private final List<Informe> informes = new ArrayList<>();

    @Override
    public void guardar(Informe informe) {
        informes.add(informe);
    }
    
    // Método auxiliar que te servirá luego para los tests
    public List<Informe> getInformes() {
        return informes;
    }
}