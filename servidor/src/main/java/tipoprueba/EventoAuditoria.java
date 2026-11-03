package tipoprueba;

import java.time.Instant;

/** Registro de una modificación de tipos de prueba (D-017). */
public class EventoAuditoria {
    public enum Accion { CREAR, MODIFICAR, DESACTIVAR }

    private final Instant fecha;
    private final String usuario;
    private final Accion accion;
    private final long idTipoPrueba;
    private final String detalle;

    public EventoAuditoria(Instant fecha, String usuario, Accion accion, long idTipoPrueba, String detalle) {
        this.fecha = fecha;
        this.usuario = usuario;
        this.accion = accion;
        this.idTipoPrueba = idTipoPrueba;
        this.detalle = detalle;
    }

    public Instant getFecha() { return fecha; }
    public String getUsuario() { return usuario; }
    public Accion getAccion() { return accion; }
    public long getIdTipoPrueba() { return idTipoPrueba; }
    public String getDetalle() { return detalle; }
}
