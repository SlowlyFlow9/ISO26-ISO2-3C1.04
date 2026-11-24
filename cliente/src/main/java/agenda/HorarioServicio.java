package agenda;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class HorarioServicio {

    private List<HorarioDia> dias = new ArrayList<>();
    private String modificadoPor;
    private LocalDateTime modificadoEn;

    public static HorarioServicio vacio() {
        HorarioServicio horario = new HorarioServicio();
        for (DayOfWeek dia : DayOfWeek.values()) {
            horario.dias.add(new HorarioDia(
                    dia, new ArrayList<>()));
        }
        return horario;
    }

    public List<HorarioDia> getDias() {
        return dias;
    }

    public void setDias(List<HorarioDia> dias) {
        this.dias = dias;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(String modificadoPor) {
        this.modificadoPor = modificadoPor;
    }

    public LocalDateTime getModificadoEn() {
        return modificadoEn;
    }

    public void setModificadoEn(LocalDateTime modificadoEn) {
        this.modificadoEn = modificadoEn;
    }
}
