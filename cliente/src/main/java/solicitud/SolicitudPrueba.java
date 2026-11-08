package solicitud;

import java.time.LocalDateTime;

/**
 * Representa los datos de una solicitud de prueba diagnóstica.
 * CU11 - Cliente: Prescribir pruebas diagnosticas.
 */
public class SolicitudPrueba {

    // Atributos de la solicitud
    private final long id;
    private final long pacienteId;
    private final long medicoId;
    private final long tipoPruebaId;

    private final String indicacionClinica;
    private final String justificacion;
    private final Prioridad prioridad;

    private final LocalDateTime fechaSolicitud;
    private final EstadoSolicitud estado;

    //Constructor
    public SolicitudPrueba(long id, long pacienteId,
            long medicoId, long tipoPruebaId,
            String indicacionClinica, String justificacion,
            Prioridad prioridad, LocalDateTime fechaSolicitud,
            EstadoSolicitud estado) {

        this.id = id;
        this.pacienteId = pacienteId;
        this.medicoId = medicoId;
        this.tipoPruebaId = tipoPruebaId;
        this.indicacionClinica = indicacionClinica;
        this.justificacion = justificacion;
        this.prioridad = prioridad;
        this.fechaSolicitud = fechaSolicitud;
        this.estado = estado;
    }

    //Metodos para consultar los atributos

    public long getId() {
        return id;
    }

    public long getPacienteId() {
        return pacienteId;
    }

    public long getMedicoId() {
        return medicoId;
    }

    public long getTipoPruebaId() {
        return tipoPruebaId;
    }

    public String getIndicacionClinica() {
        return indicacionClinica;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }
}
