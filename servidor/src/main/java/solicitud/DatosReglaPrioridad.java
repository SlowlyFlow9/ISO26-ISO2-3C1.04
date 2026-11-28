package solicitud;

public class DatosReglaPrioridad {

    private String nombre;
    private String descripcion;
    private int orden;

    public DatosReglaPrioridad() {
    }

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

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public int getOrden() {
        return orden;
    }

    public void setOrden(int orden) {
        this.orden = orden;
    }
}
