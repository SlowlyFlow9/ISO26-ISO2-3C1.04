
package solicitud;

import java.time.LocalDateTime;

/**
 * Representa una solicitud de prueba diagnóstica.
 * CU11 - Prescribir pruebas diagnósticas. Compartida con CU12 y CU13.
 */
public class SolicitudPrueba {

    private final long id;
    private final long pacienteId;
    private final long medicoId;
    private final long tipoPruebaId;

    private final String indicacionClinica;
    private final String justificacion;
    private final Prioridad prioridad;

    private final LocalDateTime fechaSolicitud;
    private final String estado;

    public SolicitudPrueba(long id, long pacienteId,
            long medicoId, long tipoPruebaId,
            String indicacionClinica, String justificacion,
            Prioridad prioridad, LocalDateTime fechaSolicitud,
            String estado) {

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

    public String getEstado() {
        return estado;
    }

     //Devuelve una nueva solicitud con otro identificador, conservando el resto de sus datos.
    public SolicitudPrueba conId(long nuevoId) {
        return new SolicitudPrueba(
            nuevoId,
            pacienteId,
            medicoId,
            tipoPruebaId,
            indicacionClinica,
            justificacion,
            prioridad,
            fechaSolicitud,
            estado
        );
    }

}
