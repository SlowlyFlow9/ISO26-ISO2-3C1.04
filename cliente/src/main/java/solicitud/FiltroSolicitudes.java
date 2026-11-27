package solicitud;

import java.util.ArrayList;
import java.util.List;

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

    //Parametros de la peticion GET /solicitudes (D-037); vacio si no hay criterios
    public String comoParametros() {
        List<String> parametros = new ArrayList<>();
        if (pacienteId != null) parametros.add("pacienteId=" + pacienteId);
        if (estado != null) parametros.add("estado=" + estado.name());
        if (prioridad != null) parametros.add("prioridad=" + prioridad.name());
        return parametros.isEmpty() ? "" : "?" + String.join("&", parametros);
    }
}
