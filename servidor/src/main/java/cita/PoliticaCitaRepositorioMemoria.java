package cita;

public class PoliticaCitaRepositorioMemoria
        implements PoliticaCitaRepositorio {

    private PoliticaCita politica = PoliticaCita.inicial();

    @Override
    public synchronized PoliticaCita consultar() {
        return politica.copia();
    }

    @Override
    public synchronized PoliticaCita guardar(PoliticaCita politica) {
        if (politica == null) {
            throw new IllegalArgumentException(
                    "La politica no puede ser nula");
        }
        this.politica = politica.copia();
        return this.politica.copia();
    }
}
