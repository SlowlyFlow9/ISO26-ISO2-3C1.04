package tipoprueba;

/** Datos de entrada para crear o modificar un tipo de prueba. */
public class DatosTipoPrueba {
    private final String nombre;
    private final String descripcion;
    private final int duracionEstimada;
    private final boolean requierePreparacion;
    private final String instruccionesPaciente;

    public DatosTipoPrueba(String nombre, String descripcion, int duracionEstimada,
                           boolean requierePreparacion, String instruccionesPaciente) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionEstimada = duracionEstimada;
        this.requierePreparacion = requierePreparacion;
        this.instruccionesPaciente = instruccionesPaciente;
    }

    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getDuracionEstimada() { return duracionEstimada; }
    public boolean isRequierePreparacion() { return requierePreparacion; }
    public String getInstruccionesPaciente() { return instruccionesPaciente; }
}
