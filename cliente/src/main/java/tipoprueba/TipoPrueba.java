package tipoprueba;

/** Modelo de la entidad TipoPrueba tal y como lo devuelve el servicio. */
public class TipoPrueba {
    private final long id;
    private final String nombre;
    private final String descripcion;
    private final int duracionEstimada; // minutos
    private final boolean requierePreparacion;
    private final String instruccionesPaciente;
    private final boolean activo;

    public TipoPrueba(long id, String nombre, String descripcion, int duracionEstimada,
                      boolean requierePreparacion, String instruccionesPaciente, boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.duracionEstimada = duracionEstimada;
        this.requierePreparacion = requierePreparacion;
        this.instruccionesPaciente = instruccionesPaciente;
        this.activo = activo;
    }

    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public int getDuracionEstimada() { return duracionEstimada; }
    public boolean isRequierePreparacion() { return requierePreparacion; }
    public String getInstruccionesPaciente() { return instruccionesPaciente; }
    public boolean isActivo() { return activo; }
}
