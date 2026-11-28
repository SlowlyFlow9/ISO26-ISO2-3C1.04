package solicitud;

public class DatosReglaPrioridad {

    private final String nombre;
    private final String descripcion;
    private final int orden;

    public DatosReglaPrioridad(
            String nombre,
            String descripcion,
            int orden) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.orden = orden;
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
}
