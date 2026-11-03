package tipoprueba;

/** Entidad central TipoPrueba. Inmutable: cada cambio genera una nueva instancia. */
public class TipoPrueba {
    private final long id;
    private final String nombre;
    private final String descripcion;
    private final int duracionEstimada; // minutos, > 0
    private final boolean requierePreparacion;
    private final String instruccionesPaciente;
    private final boolean activo; // baja lógica (D-008)

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

    public TipoPrueba conId(long nuevoId) {
        return new TipoPrueba(nuevoId, nombre, descripcion, duracionEstimada,
                requierePreparacion, instruccionesPaciente, activo);
    }

    public TipoPrueba conActivo(boolean nuevoActivo) {
        return new TipoPrueba(id, nombre, descripcion, duracionEstimada,
                requierePreparacion, instruccionesPaciente, nuevoActivo);
    }
}
