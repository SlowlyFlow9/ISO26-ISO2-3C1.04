package solicitud;

/**
 * Estados de una solicitud de prueba diagnóstica.
 * D-026: el estado inicial lo establece el servidor.
 * CU13: estados posteriores y transiciones (issue #99).
 */
public enum EstadoSolicitud {

    //Estado inicial: la solicitud existe pero no tiene cita (D-026)
    PENDIENTE_CITACION,

    //La solicitud tiene cita asignada (lo asignan CU25 y CU27)
    CITADA,

    //La prueba se ha realizado (lo asigna CU19)
    REALIZADA,

    //El medico ha anulado la solicitud (CU13)
    ANULADA;

    //CU13: solo se puede modificar o anular una solicitud pendiente de citacion
    public boolean permiteGestion() {
        return this == PENDIENTE_CITACION;
    }
}
