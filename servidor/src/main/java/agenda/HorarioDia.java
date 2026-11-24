package agenda;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;

public class HorarioDia {

    private DayOfWeek dia;
    private List<IntervaloHorario> intervalos = new ArrayList<>();

    public HorarioDia() {
    }

    public HorarioDia(
            DayOfWeek dia,
            List<IntervaloHorario> intervalos) {
        this.dia = dia;
        this.intervalos = intervalos;
    }

    public HorarioDia copia() {
        List<IntervaloHorario> copiaIntervalos =
                intervalos == null
                        ? null
                        : intervalos.stream()
                                .map(IntervaloHorario::copia)
                                .toList();
        return new HorarioDia(dia, copiaIntervalos);
    }

    public DayOfWeek getDia() {
        return dia;
    }

    public void setDia(DayOfWeek dia) {
        this.dia = dia;
    }

    public List<IntervaloHorario> getIntervalos() {
        return intervalos;
    }

    public void setIntervalos(List<IntervaloHorario> intervalos) {
        this.intervalos = intervalos;
    }
}
