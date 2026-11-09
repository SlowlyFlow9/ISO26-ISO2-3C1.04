package prueba;

public class PruebaMedica {
    private String id;
    private String estado;
    private String incidencia; // Campo necesario para el CU20

    public PruebaMedica(String id, String estado) {
        this.id = id;
        this.estado = estado;
    }

    public String getId() { return id; }
    
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    
    public String getIncidencia() { return incidencia; }
    public void setIncidencia(String incidencia) { this.incidencia = incidencia; }
}