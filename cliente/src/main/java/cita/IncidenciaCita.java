package cita;

import java.time.LocalDateTime;

public class IncidenciaCita {

    private Long id;
    private long citaId;
    private long medicoId;
    private LocalDateTime fechaHora;
    private String descripcion;

    public IncidenciaCita() {
    }

    public Long getId() {
        return id;
    }

    public long getCitaId() {
        return citaId;
    }

    public long getMedicoId() {
        return medicoId;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setCitaId(long citaId) {
        this.citaId = citaId;
    }

    public void setMedicoId(long medicoId) {
        this.medicoId = medicoId;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }
}
