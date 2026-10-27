public class TipoPrueba {
public class TipoPrueba {
    private Long id;
    private String nombre;
    private String descripcion;
    private int duracionEstimada; 
    private boolean requierePreparacion;
    private String instruccionesPaciente;
    private boolean activo = true;
 
    public TipoPrueba() { }
 
    public TipoPrueba copia() {
        TipoPrueba c = new TipoPrueba();
        c.id = id; c.nombre = nombre; c.descripcion = descripcion;
        c.duracionEstimada = duracionEstimada;
        c.requierePreparacion = requierePreparacion;
        c.instruccionesPaciente = instruccionesPaciente;
        c.activo = activo;
        return c;
    }
 
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }

    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getDuracionEstimada() { return duracionEstimada; }

    public void setDuracionEstimada(int duracionEstimada) { this.duracionEstimada = duracionEstimada; }

    public boolean isRequierePreparacion() { return requierePreparacion; }

    public void setRequierePreparacion(boolean requierePreparacion) { this.requierePreparacion = requierePreparacion; }

    public String getInstruccionesPaciente() { return instruccionesPaciente; }

    public void setInstruccionesPaciente(String instruccionesPaciente) { this.instruccionesPaciente = instruccionesPaciente; }

    public boolean isActivo() { return activo; }
    
    public void setActivo(boolean activo) {this.activo = activo; }
}
 

}
