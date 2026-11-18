package agenda;

import java.time.LocalDate;
import java.time.LocalTime;

public class AgendaMedica {

    private Long id;
    private long medicoId;
    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String observaciones;
    private boolean activo = true;

    public AgendaMedica() {
    }

    public AgendaMedica copia() {
        AgendaMedica copia = new AgendaMedica();
        copia.id = id;
        copia.medicoId = medicoId;
        copia.fecha = fecha;
        copia.horaInicio = horaInicio;
        copia.horaFin = horaFin;
        copia.observaciones = observaciones;
        copia.activo = activo;
        return copia;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getMedicoId() {
        return medicoId;
    }

    public void setMedicoId(long medicoId) {
        this.medicoId = medicoId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
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

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}