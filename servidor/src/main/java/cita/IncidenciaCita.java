package cita;

import java.time.LocalDateTime;

/** Incidencia registrada por el médico responsable de una cita. */
public class IncidenciaCita {

    private Long id;
    private long citaId;
    private long medicoId;
    private LocalDateTime fechaHora;
    private String descripcion;

    public IncidenciaCita() {
    }

    public IncidenciaCita(
            Long id,
            long citaId,
            long medicoId,
            LocalDateTime fechaHora,
            String descripcion) {
        this.id = id;
        this.citaId = citaId;
        this.medicoId = medicoId;
        this.fechaHora = fechaHora;
        this.descripcion = descripcion;
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

    IncidenciaCita conId(long nuevoId) {
        return new IncidenciaCita(
                nuevoId, citaId, medicoId, fechaHora, descripcion);
    }
}
