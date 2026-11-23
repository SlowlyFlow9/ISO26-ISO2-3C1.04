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
            horario.dias.add(new HorarioDia(dia, new ArrayList<>()));
        }
        return horario;
    }

    public HorarioServicio copia() {
        HorarioServicio copia = new HorarioServicio();
        copia.dias = dias == null
                ? null
                : dias.stream().map(HorarioDia::copia).toList();
        copia.modificadoPor = modificadoPor;
        copia.modificadoEn = modificadoEn;
        return copia;
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
