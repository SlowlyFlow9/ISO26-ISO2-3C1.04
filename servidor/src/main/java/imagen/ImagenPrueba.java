package imagen;

public class ImagenPrueba {
    private String id;
    private String idPrueba;
    private String ruta;

    public ImagenPrueba(String id, String idPrueba, String ruta) {
        this.id = id;
        this.idPrueba = idPrueba;
        this.ruta = ruta;
    }

    public String getId() { return id; }
    public String getIdPrueba() { return idPrueba; }
    public String getRuta() { return ruta; }
}