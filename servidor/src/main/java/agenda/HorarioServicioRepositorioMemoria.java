package agenda;

public class HorarioServicioRepositorioMemoria
        implements HorarioServicioRepositorio {

    private HorarioServicio horario = HorarioServicio.vacio();

    @Override
    public synchronized HorarioServicio consultar() {
        return horario.copia();
    }

    @Override
    public synchronized HorarioServicio guardar(
            HorarioServicio horario) {
        if (horario == null) {
            throw new IllegalArgumentException(
                    "El horario no puede ser nulo");
        }
        this.horario = horario.copia();
        return this.horario.copia();
    }
}
