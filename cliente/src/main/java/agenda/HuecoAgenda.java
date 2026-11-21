package agenda;

import java.time.LocalTime;

/**
 * CU29 - Modelo de un hueco de agenda en el cliente.
 *
 * Contiene los datos intercambiados con el servidor
 * mediante HTTP/JSON.
 */

public class HuecoAgenda {

    private Long id;
    private long agendaId;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private boolean bloqueado;

    public HuecoAgenda() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getAgendaId() {
        return agendaId;
    }

    public void setAgendaId(long agendaId) {
        this.agendaId = agendaId;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public boolean isBloqueado() {
        return bloqueado;
    }

    public void setBloqueado(boolean bloqueado) {
        this.bloqueado = bloqueado;
    }
}
