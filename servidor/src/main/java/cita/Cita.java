package cita;

import java.time.LocalDateTime;

public class Cita {

    private Long id;
    private long pacienteId;
    private long medicoId;
    private long tipoPruebaId;
    private LocalDateTime fechaHora;
    private EstadoCita estado;

    // CU25: hueco de agenda y solicitud de la que nace la cita (nulos si la cita no viene de CU25)
    private Long huecoId;
    private Long solicitudId;

    public Cita() {
    }

    public Cita copia() {

        Cita copia = new Cita();

        copia.id = id;
        copia.pacienteId = pacienteId;
        copia.medicoId = medicoId;
        copia.tipoPruebaId = tipoPruebaId;
        copia.fechaHora = fechaHora;
        copia.estado = estado;
        copia.huecoId = huecoId;
        copia.solicitudId = solicitudId;

        return copia;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getPacienteId() {
        return pacienteId;
    }

    public void setPacienteId(long pacienteId) {
        this.pacienteId = pacienteId;
    }

    public long getMedicoId() {
        return medicoId;
    }

    public void setMedicoId(long medicoId) {
        this.medicoId = medicoId;
    }

    public long getTipoPruebaId() {
        return tipoPruebaId;
    }

    public void setTipoPruebaId(long tipoPruebaId) {
        this.tipoPruebaId = tipoPruebaId;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public void setEstado(EstadoCita estado) {
        this.estado = estado;
    }

    public Long getHuecoId() {
        return huecoId;
    }

    public void setHuecoId(Long huecoId) {
        this.huecoId = huecoId;
    }

    public Long getSolicitudId() {
        return solicitudId;
    }

    public void setSolicitudId(Long solicitudId) {
        this.solicitudId = solicitudId;
    }
}