package solicitud;

import java.time.LocalDateTime;

public class ReglaPrioridad {

    private final long id;
    private final String nombre;
    private final String descripcion;
    private final int orden;
    private final boolean activo;
    private final String modificadoPor;
    private final LocalDateTime modificadoEn;

    public ReglaPrioridad(
            long id,
            String nombre,
            String descripcion,
            int orden,
            boolean activo,
            String modificadoPor,
            LocalDateTime modificadoEn) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.orden = orden;
        this.activo = activo;
        this.modificadoPor = modificadoPor;
        this.modificadoEn = modificadoEn;
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActivo() {
        return activo;
    }

    public String getModificadoPor() {
        return modificadoPor;
    }

    public LocalDateTime getModificadoEn() {
        return modificadoEn;
    }

    public ReglaPrioridad conId(long nuevoId) {
        return new ReglaPrioridad(
                nuevoId, nombre, descripcion, orden, activo,
                modificadoPor, modificadoEn);
    }

    public ReglaPrioridad conDatos(
            String nuevoNombre,
            String nuevaDescripcion,
            int nuevoOrden,
            String usuario,
            LocalDateTime fecha) {
        return new ReglaPrioridad(
                id, nuevoNombre, nuevaDescripcion, nuevoOrden, activo,
                usuario, fecha);
    }

    public ReglaPrioridad desactivada(
            String usuario,
            LocalDateTime fecha) {
        return new ReglaPrioridad(
                id, nombre, descripcion, orden, false, usuario, fecha);
    }
}
