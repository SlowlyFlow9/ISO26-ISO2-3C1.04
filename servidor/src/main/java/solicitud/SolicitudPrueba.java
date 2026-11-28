package solicitud;

import java.time.LocalDateTime;

/**
 * Representa una solicitud de prueba diagnostica.
 * CU11 - Prescribir pruebas diagnosticas.
 * Compartida con CU12 y CU13.
 */
public class SolicitudPrueba {

    //Atributos de la solicitud
    private final long id;
    private final long pacienteId;
    private final long medicoId;
    private final long tipoPruebaId;

    private final String indicacionClinica;
    private final String justificacion;
    private final Prioridad prioridad;

    private final LocalDateTime fechaSolicitud;
    private final EstadoSolicitud estado;

    // CU12 (D-030): periodicidad del seguimiento; todos nulos si no es un seguimiento
    private final Integer intervalo;
    private final UnidadPeriodicidad unidad;
    private final Integer numeroRepeticiones;
    private final Long solicitudOrigenId;

    // CU13: datos de la anulacion; nulos si la solicitud no esta anulada
    private final String motivoAnulacion;
    private final LocalDateTime fechaAnulacion;

    //Constructor de CU11: una solicitud sin periodicidad
    public SolicitudPrueba(long id, long pacienteId,
            long medicoId, long tipoPruebaId,
            String indicacionClinica, String justificacion,
            Prioridad prioridad, LocalDateTime fechaSolicitud,
            EstadoSolicitud estado) {

        this(id, pacienteId, medicoId, tipoPruebaId, indicacionClinica, justificacion,
                prioridad, fechaSolicitud, estado, null, null, null, null);
    }

    //Constructor de CU12: una prueba de seguimiento con su periodicidad
    public SolicitudPrueba(long id, long pacienteId,
            long medicoId, long tipoPruebaId,
            String indicacionClinica, String justificacion,
            Prioridad prioridad, LocalDateTime fechaSolicitud,
            EstadoSolicitud estado,
            Integer intervalo, UnidadPeriodicidad unidad,
            Integer numeroRepeticiones, Long solicitudOrigenId) {

        this(id, pacienteId, medicoId, tipoPruebaId, indicacionClinica, justificacion,
                prioridad, fechaSolicitud, estado, intervalo, unidad,
                numeroRepeticiones, solicitudOrigenId, null, null);
    }

    //Constructor de CU13: incluye los datos de la anulacion
    public SolicitudPrueba(long id, long pacienteId,
            long medicoId, long tipoPruebaId,
            String indicacionClinica, String justificacion,
            Prioridad prioridad, LocalDateTime fechaSolicitud,
            EstadoSolicitud estado,
            Integer intervalo, UnidadPeriodicidad unidad,
            Integer numeroRepeticiones, Long solicitudOrigenId,
            String motivoAnulacion, LocalDateTime fechaAnulacion) {

        this.motivoAnulacion = motivoAnulacion;
        this.fechaAnulacion = fechaAnulacion;
        this.intervalo = intervalo;
        this.unidad = unidad;
        this.numeroRepeticiones = numeroRepeticiones;
        this.solicitudOrigenId = solicitudOrigenId;
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

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public Integer getIntervalo() {
        return intervalo;
    }

    public UnidadPeriodicidad getUnidad() {
        return unidad;
    }

    public Integer getNumeroRepeticiones() {
        return numeroRepeticiones;
    }

    public Long getSolicitudOrigenId() {
        return solicitudOrigenId;
    }

    public String getMotivoAnulacion() {
        return motivoAnulacion;
    }

    public LocalDateTime getFechaAnulacion() {
        return fechaAnulacion;
    }

    //Una solicitud es un seguimiento si tiene periodicidad
    public boolean esSeguimiento() {
        return intervalo != null;
    }

    //CU13: el medico solo puede modificar o anular una solicitud pendiente de citacion
    public boolean sePuedeGestionar() {
        return estado != null && estado.permiteGestion();
    }

    //CU25: devuelve una copia de la solicitud con otro estado (la solicitud es inmutable)
    public SolicitudPrueba conEstado(EstadoSolicitud nuevoEstado) {
        return new SolicitudPrueba(
            id,
            pacienteId,
            medicoId,
            tipoPruebaId,
            indicacionClinica,
            justificacion,
            prioridad,
            fechaSolicitud,
            nuevoEstado,
            intervalo,
            unidad,
            numeroRepeticiones,
            solicitudOrigenId,
            motivoAnulacion,
            fechaAnulacion
        );
    }

    //CU28: devuelve una copia de la solicitud con otra prioridad (D-038); el resto se conserva
    public SolicitudPrueba conPrioridad(Prioridad nuevaPrioridad) {
        return new SolicitudPrueba(
            id,
            pacienteId,
            medicoId,
            tipoPruebaId,
            indicacionClinica,
            justificacion,
            nuevaPrioridad,
            fechaSolicitud,
            estado,
            intervalo,
            unidad,
            numeroRepeticiones,
            solicitudOrigenId,
            motivoAnulacion,
            fechaAnulacion
        );
    }

    //Devuelve una copia de la solicitud con otro id
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
            estado,
            intervalo,
            unidad,
            numeroRepeticiones,
            solicitudOrigenId,
            motivoAnulacion,
            fechaAnulacion
        );
    }
}
