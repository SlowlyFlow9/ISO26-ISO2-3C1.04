package prueba;

public class PruebaMedica {
    private String id;
    private String estado;

    public PruebaMedica(String id, String estado) {
        this.id = id;
        this.estado = estado;
    }

    public String getId() { return id; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}