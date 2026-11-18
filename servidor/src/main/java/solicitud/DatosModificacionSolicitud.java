package solicitud;

/**
 * CU13: datos que el medico puede modificar de una solicitud pendiente de citacion (D-032).
 * El paciente, el medico, el tipo de prueba, la fecha y el estado no se modifican.
 * La periodicidad solo se informa en los seguimientos; en el resto es nula.
 */
public record DatosModificacionSolicitud(
        String indicacionClinica,
        String justificacion,
        Prioridad prioridad,
        Integer intervalo,
        UnidadPeriodicidad unidad,
        Integer numeroRepeticiones) {
}
