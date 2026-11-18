package solicitud;

/**
 * CU13: criterios para consultar solicitudes. Todos son opcionales y se pueden combinar;
 * un criterio nulo significa que no se filtra por el.
 */
public record FiltroSolicitudes(Long pacienteId, EstadoSolicitud estado, Prioridad prioridad) {

    //Filtro sin criterios: devuelve todas las solicitudes
    public static FiltroSolicitudes vacio() {
        return new FiltroSolicitudes(null, null, null);
    }

    //Comprueba si una solicitud cumple todos los criterios del filtro
    public boolean cumple(SolicitudPrueba solicitud) {
        return (pacienteId == null || solicitud.getPacienteId() == pacienteId)
                && (estado == null || solicitud.getEstado() == estado)
                && (prioridad == null || solicitud.getPrioridad() == prioridad);
    }
}
